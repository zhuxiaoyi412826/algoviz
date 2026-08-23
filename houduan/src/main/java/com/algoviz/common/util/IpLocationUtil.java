package com.algoviz.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpLocationUtil {

    private static final Logger log = LoggerFactory.getLogger(IpLocationUtil.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** IP 查询缓存：IP → {location, expireAt} */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600_000L; // 1 小时

    /** 服务器公网 IP 缓存（启动时或首次调用时获取） */
    private volatile String cachedPublicIp;
    private volatile long publicIpExpireAt;

    /**
     * 根据 IP 地址识别登录地点
     * - 公网 IP：直接查询 ip-api.com 获取城市
     * - 内网 IP：先获取服务器公网 IP，再查询该 IP 的城市
     * - 最终降级：返回 IP 本身
     */
    public String getLocationByIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知";
        }

        // 先查缓存
        CacheEntry cached = cache.get(ip);
        if (cached != null && System.currentTimeMillis() < cached.expireAt) {
            return cached.location;
        }

        String location;
        if (isInternalIp(ip)) {
            // 内网 IP：获取服务器公网 IP 再查询
            String publicIp = getServerPublicIp();
            if (publicIp != null && !publicIp.isEmpty()) {
                location = queryIpApi(publicIp);
            } else {
                location = "未知位置";
            }
        } else {
            // 公网 IP：直接查询
            location = queryIpApi(ip);
        }

        if (location == null || location.isEmpty()) {
            location = ip;
        }

        // 写入缓存
        cache.put(ip, new CacheEntry(location, System.currentTimeMillis() + CACHE_TTL_MS));
        return location;
    }

    private boolean isInternalIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return true;
        if (ip.equals("127.0.0.1") || ip.startsWith("127.")) return true;
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    /**
     * 获取服务器公网 IP（通过多个 API 降级）
     */
    private String getServerPublicIp() {
        long now = System.currentTimeMillis();
        if (cachedPublicIp != null && now < publicIpExpireAt) {
            return cachedPublicIp;
        }

        String[] apis = {
                "https://api.ipify.org",
                "https://ifconfig.me/ip",
                "https://checkip.amazonaws.com"
        };

        for (String api : apis) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(api))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String publicIp = response.body().trim();
                if (publicIp != null && !publicIp.isEmpty() && publicIp.contains(".")) {
                    cachedPublicIp = publicIp;
                    publicIpExpireAt = now + CACHE_TTL_MS;
                    log.info("获取服务器公网 IP: {}", publicIp);
                    return publicIp;
                }
            } catch (Exception e) {
                log.debug("获取公网 IP 失败 ({}): {}", api, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 通过 ip-api.com 查询 IP 地理位置（免费版，每分钟 45 次）
     * 返回格式：国家 省份 城市
     */
    private String queryIpApi(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=status,countryName,regionName,city"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body.contains("\"status\":\"success\"")) {
                String country = extractJsonField(body, "countryName");
                String region = extractJsonField(body, "regionName");
                String city = extractJsonField(body, "city");

                StringBuilder sb = new StringBuilder();
                if (country != null && !country.isEmpty()) {
                    sb.append(country);
                }
                if (region != null && !region.isEmpty() && !region.equals(country)) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(region);
                }
                if (city != null && !city.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(city);
                }
                return sb.length() > 0 ? sb.toString() : null;
            }
        } catch (Exception e) {
            log.debug("ip-api.com 查询异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 简单 JSON 字段提取
     */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static class CacheEntry {
        final String location;
        final long expireAt;

        CacheEntry(String location, long expireAt) {
            this.location = location;
            this.expireAt = expireAt;
        }
    }
}
