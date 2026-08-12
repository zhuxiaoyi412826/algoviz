package com.algoviz.config;

import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    // Cookie 名称：存储用户ID（不重要凭证，可被篡改但有后端14天二次校验）
    public static final String COOKIE_USER_ID = "ALGOVIZ_UID";
    // Session 名称：存储完整用户信息（重要数据，服务端保存）
    public static final String SESSION_USER = "LOGIN_USER";
    // Cookie 有效期：4 天（单位：秒）—— 浏览器端自动过期
    public static final int COOKIE_MAX_AGE_DAYS_4 = 4 * 24 * 60 * 60;
    // 后端强制过期：14 天 —— 即便 Cookie 被篡改延长，超过也拒绝
    public static final int SESSION_MAX_DAYS_14 = 14;

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 优先检查 Session（重要数据存服务端，最可靠）
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER) != null) {
            User sessionUser = (User) session.getAttribute(SESSION_USER);
            logger.debug("Session校验通过，用户: {}", sessionUser.getUsername());
            return true;
        }

        // 2. Session 无数据，尝试通过 Cookie 自动登录
        String userIdFromCookie = getCookieValue(request, COOKIE_USER_ID);
        if (userIdFromCookie == null || userIdFromCookie.isEmpty()) {
            logger.info("无Cookie凭证，需要登录");
            writeUnauthorized(response, "未登录或凭证已过期", 40101);
            return false;
        }

        // 3. 有 Cookie，解析用户ID并校验 14 天最后登录时间
        Integer userId;
        try {
            userId = Integer.parseInt(userIdFromCookie);
        } catch (NumberFormatException e) {
            logger.warn("Cookie中用户ID格式非法: {}", userIdFromCookie);
            removeCookie(response, COOKIE_USER_ID);
            writeUnauthorized(response, "凭证非法，请重新登录", 40102);
            return false;
        }

        User user = userService.findById(userId);
        if (user == null) {
            logger.warn("Cookie对应的用户不存在: userId={}", userId);
            removeCookie(response, COOKIE_USER_ID);
            writeUnauthorized(response, "用户不存在，请重新登录", 40103);
            return false;
        }

        // 4. 关键校验：最后登录时间 > 14 天，直接拒绝（防用户手动篡改Cookie有效期）
        LocalDateTime lastLoginAt = user.getLastLoginAt();
        if (lastLoginAt == null) {
            logger.warn("用户无最后登录记录: userId={}", userId);
            removeCookie(response, COOKIE_USER_ID);
            writeUnauthorized(response, "登录状态异常，请重新登录", 40104);
            return false;
        }

        long daysSinceLastLogin = Duration.between(lastLoginAt, LocalDateTime.now()).toDays();
        if (daysSinceLastLogin > SESSION_MAX_DAYS_14) {
            logger.info("用户最后登录超过{}天（实际{}天），拒绝自动登录: userId={}",
                    SESSION_MAX_DAYS_14, daysSinceLastLogin, userId);
            removeCookie(response, COOKIE_USER_ID);
            if (session != null) {
                session.invalidate();
            }
            writeUnauthorized(response, "登录已过期，请重新登录", 40105);
            return false;
        }

        // 5. 校验通过：自动重新建立 Session（写入重要数据）
        logger.info("Cookie自动登录成功，用户: {}，距上次登录{}天", user.getUsername(), daysSinceLastLogin);
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(SESSION_USER, user);
        // 刷新 Cookie 的 4 天有效期（滑动过期）
        setCookie(response, COOKIE_USER_ID, String.valueOf(userId), COOKIE_MAX_AGE_DAYS_4);

        // cmd 控制台醒目：自动登录成功
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────┐");
        System.out.println("│            🔄  Cookie 自动免登录成功                     │");
        System.out.println("├──────────────────────────────────────────────────────────┤");
        System.out.println("│  用户       : " + pad(user.getUsername(), 42) + "│");
        System.out.println("│  距上次登录 : " + pad(daysSinceLastLogin + " 天", 42) + "│");
        System.out.println("│  Cookie 已刷新 4 天有效期                                │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();

        return true;
    }

    /**
     * 写入 401 未授权响应（JSON）
     */
    private void writeUnauthorized(HttpServletResponse response, String message, int code) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * 读取指定名称的 Cookie 值
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 设置 Cookie（HttpOnly + 同站，防XSS/CSRF）
     */
    public static void setCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // 生产环境应启用 Secure（HTTPS）
        // cookie.setSecure(true);
        response.addCookie(cookie);
    }

    /**
     * 清除 Cookie
     */
    public static void removeCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /**
     * 右侧填充空格：控制台对齐辅助
     */
    private static String pad(String str, int len) {
        if (str == null) str = "";
        if (str.length() >= len) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }
}
