package com.algoviz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 防重放攻击拦截器（nonce + 时间戳 + Redis 去重）
 *  - 请求携带 X-Nonce（随机串）与 X-Timestamp（毫秒时间戳）
 *  - 时间戳与服务器时间差超过 5 分钟 → 拒绝
 *  - 同一 nonce 在 60 秒内重复出现 → 拒绝（防重放）
 *  - 未携带 nonce 的请求放行（兼容现有前端，前端后续可渐进接入）
 */
@Component
public class NonceReplayInterceptor implements HandlerInterceptor {

    /** 时间戳允许偏差：5 分钟 */
    private static final long MAX_TIMESTAMP_DRIFT_MS = 5 * 60 * 1000L;
    /** nonce 去重窗口：60 秒 */
    private static final long NONCE_TTL_SECONDS = 60L;

    @Autowired
    private StringRedisTemplate redis;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String nonce = request.getHeader("X-Nonce");
        String timestamp = request.getHeader("X-Timestamp");
        // 兼容模式：未启用 nonce 的请求放行（渐进接入）
        if (nonce == null || nonce.isEmpty() || timestamp == null || timestamp.isEmpty()) {
            return true;
        }

        // 1. 时间戳合法性 + 时效窗口校验
        long clientTs;
        try {
            clientTs = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return reject(response, "请求时间戳非法");
        }
        if (Math.abs(System.currentTimeMillis() - clientTs) > MAX_TIMESTAMP_DRIFT_MS) {
            return reject(response, "请求时间戳已过期");
        }

        // 2. nonce 去重：SETNX 成功才放行，60 秒内重复 nonce 拒绝
        Boolean firstUse = redis.opsForValue().setIfAbsent("nonce:" + nonce, "1", NONCE_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(firstUse)) {
            return reject(response, "请求重复提交");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", 429);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
