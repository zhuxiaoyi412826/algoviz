package com.algoviz.common.util;

import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.CompletableFuture;

@Component
public class IpLocationUtil {

    private static final Logger log = LoggerFactory.getLogger(IpLocationUtil.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /** IP 查询缓存：IP → {location, expireAt} */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600_000L; // 1 小时

    /** 服务器公网 IP 缓存 */
    private volatile String cachedPublicIp;
    private volatile long publicIpExpireAt;

    /**
     * 启动时异步预热服务器公网 IP，不阻塞应用启动
     */
    @PostConstruct
    public void preWarm() {
        CompletableFuture.runAsync(() -> {
            log.info("异步预热服务器公网 IP...");
            String ip = fetchServerPublicIp();
            if (ip != null) {
                // 同时预热公网 IP 的地理位置
                String loc = fetchIpLocation(ip);
                if (loc != null) {
                    cache.put(ip, new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                    cache.put("127.0.0.1", new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                    cache.put("0:0:0:0:0:0:0:1", new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                    cache.put("::1", new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                    log.info("预热完成: 公网IP={}, 位置={}", ip, loc);
                }
            }
        });
    }

    /**
     * 根据 IP 地址识别登录地点（非阻塞设计）
     * - 内网 IP：用缓存的公网 IP 位置，缓存未就绪时立即返回"本地"
     * - 公网 IP：查缓存 → 未命中则查 ip-api.com（2 秒超时）
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

        if (isInternalIp(ip)) {
            // 内网 IP：尝试用缓存的公网 IP 位置
            // 公网 IP 预热时已把 127.0.0.1 等内网 IP 写入缓存
            if (cachedPublicIp != null) {
                CacheEntry pubCache = cache.get(cachedPublicIp);
                if (pubCache != null && System.currentTimeMillis() < pubCache.expireAt) {
                    String loc = pubCache.location;
                    cache.put(ip, new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                    return loc;
                }
                // 公网 IP 已知但位置未缓存，异步查询不阻塞登录
                if (pubCache == null) {
                    CompletableFuture.runAsync(() -> {
                        String loc = fetchIpLocation(cachedPublicIp);
                        if (loc != null && !loc.isEmpty()) {
                            cache.put(cachedPublicIp, new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                            cache.put(ip, new CacheEntry(loc, System.currentTimeMillis() + CACHE_TTL_MS));
                            log.info("异步填充内网IP位置: {}", loc);
                        }
                    });
                }
            }
            // 缓存未就绪，返回"本地"并缓存，不阻塞登录
            cache.put(ip, new CacheEntry("本地", System.currentTimeMillis() + CACHE_TTL_MS));
            return "本地";
        }

        // 公网 IP：查 ip-api.com（2 秒超时）
        String location = fetchIpApi(ip);
        if (location == null || location.isEmpty()) {
            location = ip;
        }
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
     * 获取服务器公网 IP（同步，仅内部调用）
     */
    private String fetchServerPublicIp() {
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
                        .timeout(Duration.ofSeconds(2))
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
     * 查询公网 IP 的地理位置（同步，2 秒超时）
     */
    private String fetchIpLocation(String ip) {
        return fetchIpApi(ip);
    }

    /**
     * 通过 ip-api.com 查询 IP 地理位置
     */
    private String fetchIpApi(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=status,countryName,regionName,city"))
                    .timeout(Duration.ofSeconds(2))
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
