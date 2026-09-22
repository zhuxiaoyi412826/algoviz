package com.algoviz.service;

import com.algoviz.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 轻量设备指纹服务（登录风控）
 *
 * <p>指纹 = SHA-256(User-Agent + IP 段 + Accept-Language) 前 128 位（32 位十六进制）。
 * IP 取「段」而不是完整地址：同一路由器/NAT 出口下的设备视为同网络，避免动态 IP 频繁误报。</p>
 *
 * <p>记忆载体为 Redis（key: algoviz:login:device:{userId}:{fingerprint}，TTL 30 天），
 * 不新增数据表；首次见到该组合即视为「新设备/新网络」，由登录风控发送提醒邮件。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private final StringRedisTemplate redis;

    /** 识别结果 */
    public static class FingerprintResult {
        /** 指纹值（32 位十六进制） */
        public String fingerprint;
        /** 是否为首次出现的新设备/新网络 */
        public boolean newDevice;
    }

    /**
     * 识别设备并记录：首次出现返回 newDevice=true 并写入 Redis；否认知晓返回 false。
     * @param userId 本地用户 ID（为空时只计算指纹不记录）
     */
    public FingerprintResult recognize(Integer userId, String userAgent, String ip, String acceptLanguage) {
        FingerprintResult result = new FingerprintResult();
        result.fingerprint = fingerprint(userAgent, ip, acceptLanguage);
        if (userId == null) {
            return result;
        }
        String key = SecurityConstants.DEVICE_FINGERPRINT_KEY + userId + ":" + result.fingerprint;
        try {
            Boolean firstSeen = redis.opsForValue().setIfAbsent(key, String.valueOf(System.currentTimeMillis()),
                    SecurityConstants.DEVICE_FINGERPRINT_TTL_DAYS, TimeUnit.DAYS);
            result.newDevice = Boolean.TRUE.equals(firstSeen);
        } catch (Exception e) {
            // Redis 异常不阻断登录：退化为「非新设备」，只是不发提醒
            log.warn("[DeviceFingerprint] 设备指纹记录失败, userId={}, err={}", userId, e.getMessage());
        }
        return result;
    }

    /** 计算指纹（不落库） */
    public String fingerprint(String userAgent, String ip, String acceptLanguage) {
        String raw = normalize(userAgent) + "|" + ipSegment(ip) + "|" + normalize(acceptLanguage);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 必然支持 SHA-256，兜底用 hashCode 保证登录流程不中断
            return Integer.toHexString(raw.hashCode());
        }
    }

    /** IP 段：IPv4 取前三段；IPv6 取前四组；无法解析时用原值 */
    private String ipSegment(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "0";
        }
        String value = ip.trim();
        if (value.contains(":")) {
            String[] groups = value.split(":");
            int len = Math.min(4, groups.length);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(groups[i]);
            }
            return sb.toString();
        }
        String[] octets = value.split("\\.");
        if (octets.length == 4) {
            return octets[0] + "." + octets[1] + "." + octets[2];
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}