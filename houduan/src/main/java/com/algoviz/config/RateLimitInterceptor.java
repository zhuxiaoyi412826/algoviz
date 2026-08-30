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
 * 接口级 IP 限流（Redis 计数器实现，防 CC 攻击）
 *  - 写请求（POST/PUT/DELETE）：每 IP 每秒最多 20 次
 *  - 读请求（GET）：每 IP 每秒最多 100 次
 *  - 超过阈值返回 429
 *  注意：IP 优先取 nginx 注入的 X-Real-IP（可信来源），其次 X-Forwarded-For，最后 remoteAddr
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int WRITE_LIMIT = 20;
    private static final int READ_LIMIT = 100;

    @Autowired
    private StringRedisTemplate redis;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        boolean isWrite = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
        int limit = isWrite ? WRITE_LIMIT : READ_LIMIT;

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String key = "ratelimit:" + ip + ":" + path + ":" + (isWrite ? "w" : "r");

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, 1, TimeUnit.SECONDS);
        }

        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("code", 429);
            body.put("message", "请求过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        // 生产 nginx 会设置可信的 X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
