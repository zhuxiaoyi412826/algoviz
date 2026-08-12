package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/login")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "登录管理", description = "登录相关接口")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserService userService;

    @PostMapping
    @Operation(summary = "登录", description = "通过验证码登录（兼容老版本接口）")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/account")
    @Operation(summary = "账号密码登录", description = "通过用户名和密码登录；登录成功会下发4天Cookie并建立Session")
    public LoginResponse loginByAccount(@RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        LoginResponse loginResponse = loginService.loginByAccount(request.getUsername(), request.getPassword());
        if (loginResponse.isSuccess() && loginResponse.getUserInfo() != null) {
            Integer userId = loginResponse.getUserInfo().getId();
            User user = userService.findById(userId);
            if (user != null) {
                // 1. 重要数据写入 Session
                if (user.getLastLoginAt() == null) {
                    user.setLastLoginAt(LocalDateTime.now());
                }
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(AuthInterceptor.SESSION_USER, user);
                logger.info("账号密码登录成功，Session已创建，用户: {}", user.getUsername());

                // 2. Cookie 下发 4 天有效期
                AuthInterceptor.setCookie(response,
                        AuthInterceptor.COOKIE_USER_ID,
                        String.valueOf(userId),
                        AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);
                logger.info("账号密码登录成功，Cookie已下发（4天有效期），userId: {}", userId);

                // 3. cmd 控制台卡片
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║            🔑  账号密码登录成功                          ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  用户ID     : " + padRight(String.valueOf(user.getId()), 41) + "║");
                System.out.println("║  用户名     : " + padRight(user.getUsername(), 41) + "║");
                System.out.println("║  昵称       : " + padRight(user.getNickname(), 41) + "║");
                System.out.println("║  邮箱       : " + padRight(user.getEmail(), 41) + "║");
                System.out.println("║  登录时间   : " + padRight(LocalDateTime.now().toString(), 41) + "║");
                System.out.println("║  Cookie有效 : 4 天（自动免登录）                         ║");
                System.out.println("║  强制过期   : 14 天后需重新验证账号                      ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  👉 即将打开用户个人中心界面...                          ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();

                // 4. 返回个人界面跳转路径
                loginResponse.setRedirectUrl("/user/profile?id=" + userId);
            }
        } else {
            logger.warn("账号密码登录失败: {} - {}", request.getUsername(), loginResponse.getMessage());
        }
        return loginResponse;
    }

    @GetMapping("/verification-code")
    @Operation(summary = "获取验证码", description = "生成登录验证码")
    public String getVerificationCode() {
        return loginService.generateVerificationCode();
    }

    @GetMapping("/check-status")
    @Operation(summary = "检查登录状态", description = "轮询检查验证码是否被微信扫码确认；登录成功会下发4天Cookie并建立Session")
    public LoginResponse checkStatus(@RequestParam String code,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        LoginResponse loginResponse = loginService.checkLoginStatus(code);
        if (loginResponse.isSuccess() && loginResponse.getUserInfo() != null) {
            Integer userId = loginResponse.getUserInfo().getId();
            // 1. 重要数据写入 Session（服务端保存）
            User user = userService.findById(userId);
            if (user != null) {
                // 确保 lastLoginAt 已更新（登录时Service已调用updateLastLogin）
                if (user.getLastLoginAt() == null) {
                    user.setLastLoginAt(LocalDateTime.now());
                }
                HttpSession session = request.getSession(true);
                session.setAttribute(AuthInterceptor.SESSION_USER, user);
                logger.info("登录成功，Session已创建，用户: {}", user.getUsername());

                // 2. 不重要凭证写入 Cookie，4 天有效期（浏览器自动过期）
                AuthInterceptor.setCookie(response,
                        AuthInterceptor.COOKIE_USER_ID,
                        String.valueOf(userId),
                        AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);
                logger.info("登录成功，Cookie已下发（4天有效期），userId: {}", userId);

                // 3. cmd 控制台醒目的登录成功消息
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║            🎉  微信公众号用户登录成功  🎉                ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  用户ID     : " + padRight(String.valueOf(user.getId()), 41) + "║");
                System.out.println("║  用户名     : " + padRight(user.getUsername(), 41) + "║");
                System.out.println("║  昵称       : " + padRight(user.getNickname(), 41) + "║");
                System.out.println("║  邮箱       : " + padRight(user.getEmail(), 41) + "║");
                System.out.println("║  登录时间   : " + padRight(LocalDateTime.now().toString(), 41) + "║");
                System.out.println("║  Cookie有效 : 4 天（自动免登录）                         ║");
                System.out.println("║  强制过期   : 14 天后需重新验证账号                      ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  👉 即将打开用户个人中心界面...                          ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();

                // 4. 返回个人界面跳转路径（前端据此打开用户界面）
                loginResponse.setRedirectUrl("/user/profile?id=" + userId);
            }
        } else if (!loginResponse.isSuccess()) {
            // 登录未就绪/失败，打印细节（非错误级别，正常轮询）
            if (!"等待扫码".equals(loginResponse.getMessage())) {
                logger.debug("check-status 结果: {} - {}", loginResponse.isSuccess(), loginResponse.getMessage());
            }
        }
        return loginResponse;
    }

    /**
     * 右侧填充空格，用于控制台表格对齐
     */
    private static String padRight(String str, int len) {
        if (str == null) str = "";
        if (str.length() >= len) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "清除Cookie和Session，返回登录页")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        String logoutUsername = "匿名";
        // 清除 Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object user = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (user instanceof User) {
                logoutUsername = ((User) user).getUsername();
                logger.info("用户登出: {}", logoutUsername);
            }
            session.invalidate();
        }
        // 清除 Cookie
        AuthInterceptor.removeCookie(response, AuthInterceptor.COOKIE_USER_ID);

        // cmd 控制台醒目的登出消息
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────┐");
        System.out.println("│            🚪  用户已登出                                 │");
        System.out.println("├──────────────────────────────────────────────────────────┤");
        System.out.println("│  用户名     : " + padRight(logoutUsername, 42) + "│");
        System.out.println("│  时间       : " + padRight(LocalDateTime.now().toString(), 42) + "│");
        System.out.println("│  Session / Cookie 已清除                                 │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();

        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户", description = "从Session或Cookie自动登录后返回用户信息")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        User user = null;
        if (session != null) {
            user = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        }
        if (user == null) {
            // 拦截器已处理自动登录，能到这里说明已校验通过
            String uid = AuthInterceptor.getCookieValue(request, AuthInterceptor.COOKIE_USER_ID);
            if (uid != null) {
                user = userService.findById(Integer.parseInt(uid));
            }
        }
        if (user != null) {
            result.put("success", true);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("age", user.getAge());
            userInfo.put("avatarUrl", user.getAvatarUrl());
            userInfo.put("lastLoginAt", user.getLastLoginAt());
            result.put("data", userInfo);
        } else {
            result.put("success", false);
            result.put("message", "未登录");
        }
        return result;
    }
}
