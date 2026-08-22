package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.EmailService;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import com.algoviz.utils.PasswordEncoderUtil;
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

    @Autowired
    private EmailService emailService;

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
        LoginResponse loginResponse;

        if (request.getCaptcha() != null && !request.getCaptcha().isEmpty()) {
            if (!CaptchaController.verifyCaptchaInSession(request.getCaptcha(), httpRequest)) {
                loginResponse = new LoginResponse();
                loginResponse.setSuccess(false);
                loginResponse.setMessage("验证码错误或已过期");
                return loginResponse;
            }
        }

        loginResponse = loginService.loginByAccount(request.getUsername(), request.getPassword());
        if (loginResponse.isSuccess() && loginResponse.getUserInfo() != null) {
            Integer userId = loginResponse.getUserInfo().getId();
            User user = userService.findById(userId);
            if (user != null) {
                if (user.getLastLoginAt() == null) {
                    user.setLastLoginAt(LocalDateTime.now());
                }
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(AuthInterceptor.SESSION_USER, user);

                AuthInterceptor.setCookie(response,
                        AuthInterceptor.COOKIE_USER_ID,
                        String.valueOf(userId),
                        AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);

                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║            🔑  账号密码登录成功                          ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  用户ID     : " + padRight(String.valueOf(user.getId()), 41) + "║");
                System.out.println("║  用户名     : " + padRight(user.getUsername(), 41) + "║");
                System.out.println("║  邮箱       : " + padRight(user.getEmail(), 41) + "║");
                System.out.println("║  登录时间   : " + padRight(LocalDateTime.now().toString(), 41) + "║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();

                loginResponse.setRedirectUrl("/user/profile?id=" + userId);
            }
        }
        return loginResponse;
    }

    @PostMapping("/send-email-code")
    @Operation(summary = "发送邮箱验证码", description = "发送邮箱验证码到指定邮箱，需要先通过图形验证码校验")
    public Map<String, Object> sendEmailCode(@RequestBody Map<String, String> body,
                                              HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        String email = body.get("email");
        String captcha = body.get("captcha");

        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入邮箱");
            return result;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        if (captcha == null || captcha.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入图形验证码");
            return result;
        }

        if (!CaptchaController.verifyCaptchaInSession(captcha, httpRequest)) {
            result.put("success", false);
            result.put("message", "图形验证码错误或已过期");
            return result;
        }

        long remaining = emailService.getSendIntervalRemaining(email);
        if (remaining > 0) {
            result.put("success", false);
            result.put("message", "请" + (remaining / 1000) + "秒后再发送");
            return result;
        }

        boolean sent = emailService.sendVerificationCode(email.trim());
        if (sent) {
            result.put("success", true);
            result.put("message", "验证码已发送到邮箱");
        } else {
            result.put("success", false);
            result.put("message", "发送失败，请稍后重试");
        }

        return result;
    }

    @PostMapping("/email-code")
    @Operation(summary = "邮箱验证码登录", description = "通过邮箱验证码登录，新用户自动注册")
    public LoginResponse loginByEmailCode(@RequestBody Map<String, String> body,
                                           HttpServletRequest httpRequest,
                                           HttpServletResponse response) {
        LoginResponse loginResponse = new LoginResponse();
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || email.trim().isEmpty()) {
            loginResponse.setSuccess(false);
            loginResponse.setMessage("请输入邮箱");
            return loginResponse;
        }

        if (code == null || code.trim().isEmpty()) {
            loginResponse.setSuccess(false);
            loginResponse.setMessage("请输入验证码");
            return loginResponse;
        }

        if (!emailService.verifyCode(email.trim(), code.trim())) {
            loginResponse.setSuccess(false);
            loginResponse.setMessage("验证码错误或已过期");
            return loginResponse;
        }

        User user = userService.findByEmail(email.trim());

        if (user == null) {
            // 新用户自动注册，用户名为 邮箱-01, 邮箱-02, ...
            String baseUsername = email.trim().split("@")[0];
            String username = generateUniqueUsername(baseUsername);

            user = new User();
            user.setUsername(username);
            user.setEmail(email.trim());
            user.setPassword(PasswordEncoderUtil.md5Encode("email_auto_register"));
            user.setNickname(username);
            user.setStatus(1);
            user.setLoginStatus("offline");
            user.setCoins(100);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            user = userService.createUser(user);
            logger.info("邮箱登录自动注册新用户: {} -> {}", email, username);

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║        📧  邮箱验证码登录 - 新用户自动注册              ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  用户名     : " + padRight(username, 41) + "║");
            System.out.println("║  邮箱       : " + padRight(email, 41) + "║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
        } else {
            if (user.getStatus() != null && user.getStatus() == 0) {
                loginResponse.setSuccess(false);
                loginResponse.setMessage("账号已被禁用，请联系管理员");
                return loginResponse;
            }
        }

        if (user.getLastLoginAt() == null) {
            user.setLastLoginAt(LocalDateTime.now());
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_USER, user);

        AuthInterceptor.setCookie(response,
                AuthInterceptor.COOKIE_USER_ID,
                String.valueOf(user.getId()),
                AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);

        loginResponse.setSuccess(true);
        loginResponse.setMessage("登录成功");
        loginResponse.setToken("email_code_token_" + System.currentTimeMillis());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setAge(user.getAge());
        userInfo.setNickname(user.getNickname());
        loginResponse.setUserInfo(userInfo);
        loginResponse.setRedirectUrl("/user/profile?id=" + user.getId());

        return loginResponse;
    }

    private String generateUniqueUsername(String base) {
        int counter = 1;
        String username;
        do {
            username = base + "-" + String.format("%02d", counter);
            counter++;
        } while (userService.findByUsername(username) != null);
        return username;
    }

    @GetMapping("/verification-code")
    @Operation(summary = "获取验证码", description = "生成登录验证码")
    public String getVerificationCode() {
        return loginService.generateVerificationCode();
    }

    @GetMapping("/check-status")
    @Operation(summary = "检查登录状态", description = "轮询检查验证码是否被微信扫码确认")
    public LoginResponse checkStatus(@RequestParam String code,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        LoginResponse loginResponse = loginService.checkLoginStatus(code);
        if (loginResponse.isSuccess() && loginResponse.getUserInfo() != null) {
            Integer userId = loginResponse.getUserInfo().getId();
            User user = userService.findById(userId);
            if (user != null) {
                if (user.getLastLoginAt() == null) {
                    user.setLastLoginAt(LocalDateTime.now());
                }
                HttpSession session = request.getSession(true);
                session.setAttribute(AuthInterceptor.SESSION_USER, user);

                AuthInterceptor.setCookie(response,
                        AuthInterceptor.COOKIE_USER_ID,
                        String.valueOf(userId),
                        AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);

                loginResponse.setRedirectUrl("/user/profile?id=" + userId);
            }
        }
        return loginResponse;
    }

    @PostMapping("/email")
    @Operation(summary = "邮箱密码登录", description = "通过邮箱和密码登录")
    public LoginResponse loginByEmail(@RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse response) {
        LoginResponse loginResponse;

        if (request.getCaptcha() != null && !request.getCaptcha().isEmpty()) {
            if (!CaptchaController.verifyCaptchaInSession(request.getCaptcha(), httpRequest)) {
                loginResponse = new LoginResponse();
                loginResponse.setSuccess(false);
                loginResponse.setMessage("验证码错误或已过期");
                return loginResponse;
            }
        }

        loginResponse = loginService.loginByEmail(request.getEmail(), request.getPassword());
        if (loginResponse.isSuccess() && loginResponse.getUserInfo() != null) {
            Integer userId = loginResponse.getUserInfo().getId();
            User user = userService.findById(userId);
            if (user != null) {
                if (user.getLastLoginAt() == null) {
                    user.setLastLoginAt(LocalDateTime.now());
                }
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(AuthInterceptor.SESSION_USER, user);

                AuthInterceptor.setCookie(response,
                        AuthInterceptor.COOKIE_USER_ID,
                        String.valueOf(userId),
                        AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);

                loginResponse.setRedirectUrl("/user/profile?id=" + userId);
            }
        }
        return loginResponse;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过用户名、邮箱、密码注册新用户")
    public Map<String, Object> register(@RequestBody Map<String, String> body,
                                        HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String captcha = body.get("captcha");

        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入用户名");
            return result;
        }
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入邮箱");
            return result;
        }
        if (password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入密码");
            return result;
        }
        if (password.length() < 6) {
            result.put("success", false);
            result.put("message", "密码长度至少6位");
            return result;
        }
        if (captcha == null || captcha.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入验证码");
            return result;
        }

        if (!CaptchaController.verifyCaptchaInSession(captcha, httpRequest)) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        User existingUser = userService.findByUsername(username);
        if (existingUser != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        User existingEmail = userService.findByEmail(email);
        if (existingEmail != null) {
            result.put("success", false);
            result.put("message", "邮箱已被注册");
            return result;
        }

        try {
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(PasswordEncoderUtil.md5Encode(password));
            newUser.setNickname(username.trim());
            newUser.setStatus(1);
            newUser.setLoginStatus("offline");
            newUser.setCoins(100);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            userService.createUser(newUser);

            logger.info("新用户注册成功: {}, {}", username, email);
            result.put("success", true);
            result.put("message", "注册成功，请登录");
        } catch (Exception e) {
            logger.error("注册失败", e);
            result.put("success", false);
            result.put("message", "注册失败，请稍后重试");
        }

        return result;
    }

    private static String padRight(String str, int len) {
        if (str == null) str = "";
        if (str.length() >= len) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "清除Cookie和Session")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        AuthInterceptor.removeCookie(response, AuthInterceptor.COOKIE_USER_ID);
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
            result.put("data", userInfo);
        } else {
            result.put("success", false);
            result.put("message", "未登录");
        }
        return result;
    }
}
