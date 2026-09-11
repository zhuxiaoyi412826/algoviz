package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.AccountStatusService;
import com.algoviz.service.EmailService;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import com.algoviz.common.util.PasswordEncoderUtil;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/login")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "登录管理", description = "登录相关接口")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 统一的时间输出格式。
     * 禁止使用 LocalDateTime.toString().substring(0, 19)：
     * 秒为 0 时 toString() 会省略 ":00" 输出 16 字符（如 "2026-09-04T19:43"），
     * substring(0, 19) 将抛 StringIndexOutOfBoundsException 导致接口 500。
     */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AccountStatusService accountStatusService;

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

                // 登录成功 -> 标记在线(0)
                userService.updateLoginStatus(userId, 0);

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
        // 注销冷静期内本次登录是否自动撤销了注销（用于成功提示）
        boolean cancellationRevoked = false;

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
            // 墓碑账号拦截：已逻辑删除/注销的邮箱不可自动注册（用户名/邮箱永久占用）
            User tombstone = userService.findByEmailIncludeDeleted(email.trim());
            if (tombstone != null) {
                loginResponse.setSuccess(false);
                if (tombstone.getIsDeleted() != null && tombstone.getIsDeleted() == 1) {
                    loginResponse.setMessage("账号不存在");
                } else if (tombstone.getStatus() != null && tombstone.getStatus() == -1) {
                    loginResponse.setMessage("账号已注销");
                } else {
                    loginResponse.setMessage("账号异常，请联系管理员");
                }
                return loginResponse;
            }
            // 新用户自动注册，用户名为 邮箱-01, 邮箱-02, ...
            String baseUsername = email.trim().split("@")[0];
            String username = generateUniqueUsername(baseUsername);

            user = new User();
            user.setUsername(username);
            user.setEmail(email.trim());
            user.setPassword(PasswordEncoderUtil.md5Encode("email_auto_register"));
            user.setNickname(username);
            user.setStatus(1);
            user.setLoginStatus(1);   // 新用户默认离线（登录成功即置0在线）
            user.setCoins(1000);
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
            // 状态校验：封禁拒绝；注销冷静期内登录自动撤销注销并放行
            AccountStatusService.LoginCheck chk = accountStatusService.checkForLogin(user);
            if (!chk.isAllowed()) {
                loginResponse.setSuccess(false);
                loginResponse.setMessage(chk.getRejectReason());
                return loginResponse;
            }
            cancellationRevoked = chk.isCancellationRevoked();
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

        // 登录成功 -> 标记在线(0)
        userService.updateLoginStatus(user.getId(), 0);

        loginResponse.setSuccess(true);
        loginResponse.setMessage(cancellationRevoked
                ? "登录成功，已为你取消账号注销申请" : "登录成功");
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
            // 含已删除/注销账号查重，避免撞唯一索引
        } while (userService.findByUsernameIncludeDeleted(username) != null);
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
                // 微信扫码登录成功 -> 标记在线(0)
                userService.updateLoginStatus(userId, 0);

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
                // 邮箱密码登录成功 -> 标记在线(0)
                userService.updateLoginStatus(userId, 0);

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

        User existingUser = userService.findByUsernameIncludeDeleted(username);
        if (existingUser != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        User existingEmail = userService.findByEmailIncludeDeleted(email);
        if (existingEmail != null) {
            result.put("success", false);
            result.put("message", "邮箱已被注册");
            return result;
        }

        try {
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(PasswordEncoderUtil.bcryptEncode(password));
            newUser.setNickname(username.trim());
            newUser.setStatus(1);
            newUser.setLoginStatus(1);   // 新用户默认离线
            newUser.setCoins(1000);
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
            // 登出 -> 标记离线(1)
            Object loginUser = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (loginUser instanceof User) {
                Integer uid = ((User) loginUser).getId();
                if (uid != null) {
                    userService.updateLoginStatus(uid, 1);
                }
            }
            session.invalidate();
        }
        AuthInterceptor.removeCookie(response, AuthInterceptor.COOKIE_USER_ID);
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    @PostMapping("/cancel-account")
    @Operation(summary = "注销账号", description = "已登录用户申请注销：校验密码后进入15天冷静期（status=-1、记录cancel_at、强制下线）；冷静期内登录自动撤销注销，逾期未登录正式注销；数据保留、后台可见，用户名/邮箱永久不可再注册")
    public Map<String, Object> cancelAccount(@RequestBody Map<String, String> body,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取当前登录用户（Session → Cookie）
        HttpSession session = request.getSession(false);
        User user = null;
        if (session != null) {
            Object attr = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (attr instanceof User) {
                user = (User) attr;
            }
        }
        if (user == null) {
            String uid = AuthInterceptor.getCookieValue(request, AuthInterceptor.COOKIE_USER_ID);
            if (uid != null) {
                try {
                    user = userService.findById(Integer.parseInt(uid));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        // 2. 二次身份校验：有密码的账号必须验密码；微信注册账号（密码为空）登录态即身份
        User dbUser = userService.findById(user.getId());
        if (dbUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == -1) {
            // 冷静期内可再次提交注销（重新计15天）；已超期/历史永久注销账号拒绝
            LocalDateTime cancelAt = dbUser.getCancelAt();
            boolean inCooling = cancelAt != null
                    && LocalDateTime.now().isBefore(cancelAt.plusDays(AccountStatusService.CANCEL_COOLING_DAYS));
            if (!inCooling) {
                result.put("success", false);
                result.put("message", "账号已注销");
                return result;
            }
            logger.info("用户在注销冷静期内再次提交申请，冷静期重新计15天: userId={}, username={}",
                    dbUser.getId(), dbUser.getUsername());
        }
        String password = body.get("password");
        if (dbUser.getPassword() != null && !dbUser.getPassword().isEmpty()) {
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请输入登录密码以确认注销");
                return result;
            }
            if (!PasswordEncoderUtil.autoMatches(password, dbUser.getPassword())) {
                result.put("success", false);
                result.put("message", "密码错误，注销已取消");
                return result;
            }
        }

        // 3. 提交注销申请：status=-1、cancel_at=NOW()（15天冷静期起点）、强制下线（数据保留，后台用户管理仍可见）
        userService.cancelAccount(user.getId());
        logger.warn("用户提交注销申请（15天冷静期开始）: userId={}, username={}", user.getId(), user.getUsername());

        // 4. 销毁会话与 Cookie（与改密踢下线一致）
        if (session != null) {
            session.invalidate();
        }
        AuthInterceptor.removeCookie(response, AuthInterceptor.COOKIE_USER_ID);

        result.put("success", true);
        result.put("message", "注销申请已提交，账号进入15天冷静期；期间登录将自动撤销注销，逾期未登录则正式注销");
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
            userInfo.put("age", user.getAge());
            userInfo.put("gender", user.getGender());
            userInfo.put("avatarUrl", user.getAvatarUrl());
            userInfo.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
            userInfo.put("coins", user.getCoins() != null ? user.getCoins() : 0);
            userInfo.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().format(DATETIME_FMT) : null);
            userInfo.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().format(DATETIME_FMT) : null);
            result.put("data", userInfo);
        } else {
            result.put("success", false);
            result.put("message", "未登录");
        }
        return result;
    }

    /**
     * 个人中心修改资料：邮箱 + 昵称
     * 约束：昵称非空且 <=30；邮箱格式合法；邮箱唯一（含已注销/删除也占用，但排除本人）
     */
    @PostMapping("/update-profile")
    @Operation(summary = "修改个人资料", description = "已登录用户修改邮箱与昵称")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取当前登录用户（Session 优先，Cookie 兜底，与 /me 口径一致）
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
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        // 2. 昵称校验
        String nickname = body.get("nickname") == null ? "" : body.get("nickname").trim();
        if (nickname.isEmpty()) {
            result.put("success", false);
            result.put("message", "昵称不能为空");
            return result;
        }
        if (nickname.length() > 30) {
            result.put("success", false);
            result.put("message", "昵称长度不能超过30个字符");
            return result;
        }
        if (nickname.matches(".*[\\r\\n\\t\\u0000-\\u001F].*")) {
            result.put("success", false);
            result.put("message", "昵称含非法字符");
            return result;
        }

        // 3. 邮箱格式校验
        String email = body.get("email") == null ? "" : body.get("email").trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        // 4. 邮箱唯一性校验（改为他人已使用的邮箱则拒绝）
        if (!email.equalsIgnoreCase(user.getEmail())) {
            User owner = userService.findByEmailIncludeDeleted(email);
            if (owner != null && !owner.getId().equals(user.getId())) {
                result.put("success", false);
                result.put("message", "该邮箱已被使用");
                return result;
            }
        }

        // 4.1 性别校验（空 = 保密；仅允许 0=女 1=男）
        String genderStr = body.get("gender");
        Integer gender = null;
        if (genderStr != null && !genderStr.trim().isEmpty()) {
            try {
                int g = Integer.parseInt(genderStr.trim());
                if (g != 0 && g != 1) {
                    result.put("success", false);
                    result.put("message", "性别取值不合法");
                    return result;
                }
                gender = g;
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("message", "性别取值不合法");
                return result;
            }
        }

        // 4.2 头像校验（支持 http(s) 图片地址或单个 emoji；空=清空使用默认）
        String avatarUrl = body.get("avatarUrl") == null ? "" : body.get("avatarUrl").trim();
        if (!avatarUrl.isEmpty()) {
            if (avatarUrl.length() > 500) {
                result.put("success", false);
                result.put("message", "头像地址过长");
                return result;
            }
            if (avatarUrl.matches(".*[\\r\\n\\t\\u0000-\\u001F].*")) {
                result.put("success", false);
                result.put("message", "头像含非法字符");
                return result;
            }
        }

        // 5. 更新资料
        userService.updateProfile(user.getId(), nickname, email, gender, avatarUrl.isEmpty() ? null : avatarUrl);

        // 6. 刷新 Session 中的用户信息（避免后续 /me 读到旧昵称/旧邮箱）
        User fresh = userService.findById(user.getId());
        if (fresh != null && session != null) {
            session.setAttribute(AuthInterceptor.SESSION_USER, fresh);
        }

        result.put("success", true);
        result.put("message", "保存成功");
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", nickname);
        data.put("email", email);
        data.put("gender", gender);
        data.put("avatarUrl", avatarUrl.isEmpty() ? null : avatarUrl);
        result.put("data", data);
        return result;
    }

    /**
     * 首次设置登录密码：仅允许当前无密码的账号（OAuth/微信扫码自动注册等）
     * 设置成功后按修改密码同策略强制重新登录
     */
    @PostMapping("/set-password")
    @Operation(summary = "设置登录密码", description = "当前无密码的账号首次设置登录密码（免旧密码）")
    public Map<String, Object> setPassword(@RequestBody Map<String, String> body,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
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
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        User dbUser = userService.findById(user.getId());
        if (dbUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        if (dbUser.getPassword() != null && !dbUser.getPassword().isEmpty()) {
            result.put("success", false);
            result.put("message", "该账号已设置过密码，请使用「修改密码」");
            return result;
        }

        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入新密码");
            return result;
        }
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            result.put("success", false);
            result.put("message", "密码长度需在6-32位之间");
            return result;
        }
        if (!newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*")) {
            result.put("success", false);
            result.put("message", "密码需包含大小写字母");
            return result;
        }
        if (!newPassword.matches(".*\\d.*")) {
            result.put("success", false);
            result.put("message", "密码需包含数字");
            return result;
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            result.put("success", false);
            result.put("message", "密码需包含特殊字符");
            return result;
        }
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            result.put("success", false);
            result.put("message", "两次输入的密码不一致");
            return result;
        }

        String encoded = PasswordEncoderUtil.bcryptEncode(newPassword);
        userService.updatePassword(dbUser.getId(), encoded);
        logger.info("账号首次设置密码成功: userId={}", dbUser.getId());

        // 与修改密码同策略：标记离线并销毁会话，强制重新登录
        userService.updateLoginStatus(dbUser.getId(), 1);
        if (session != null) {
            session.invalidate();
        }
        AuthInterceptor.removeCookie(response, AuthInterceptor.COOKIE_USER_ID);

        result.put("success", true);
        result.put("message", "密码设置成功，请重新登录");
        return result;
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "已登录用户修改密码")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        // 1. 获取当前用户
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
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        // 2. 参数校验
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入旧密码");
            return result;
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入新密码");
            return result;
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请确认新密码");
            return result;
        }

        // 3. 校验旧密码
        User dbUser = userService.findById(user.getId());
        if (dbUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        // 旧密码校验：自动探测存储算法（Argon2id / BCrypt / MD5），不再兼容明文密码
        boolean passwordValid = PasswordEncoderUtil.autoMatches(oldPassword, dbUser.getPassword());
        if (!passwordValid) {
            result.put("success", false);
            result.put("message", "旧密码错误");
            return result;
        }

        // 4. 校验新密码复杂度
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            result.put("success", false);
            result.put("message", "密码长度需在6-32位之间");
            return result;
        }
        if (!newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*")) {
            result.put("success", false);
            result.put("message", "密码需包含大小写字母");
            return result;
        }
        if (!newPassword.matches(".*\\d.*")) {
            result.put("success", false);
            result.put("message", "密码需包含数字");
            return result;
        }
        if (newPassword.matches(".*[A-Za-z0-9].*")) {
            // 必须包含特殊字符
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            result.put("success", false);
            result.put("message", "密码需包含特殊字符");
            return result;
        }

        // 5. 校验两次密码一致
        if (!newPassword.equals(confirmPassword)) {
            result.put("success", false);
            result.put("message", "两次输入的密码不一致");
            return result;
        }

        // 6. 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            result.put("success", false);
            result.put("message", "新密码不能与旧密码相同");
            return result;
        }

        // 7. 更新密码（BCrypt）
        String encodedPassword = PasswordEncoderUtil.bcryptEncode(newPassword);
        userService.updatePassword(user.getId(), encodedPassword);

        // 8. 发送通知邮件
        if (dbUser.getEmail() != null) {
            try {
                String emailContent = "您的账号密码已修改成功。\n" +
                        "修改时间：" + LocalDateTime.now().format(DATETIME_FMT) + "\n" +
                        "如非本人操作，请立即联系管理员冻结账号。";
                emailService.sendNotificationEmail(dbUser.getEmail(), "密码修改通知", emailContent);
            } catch (Exception e) {
                logger.warn("发送密码修改通知邮件失败: {}", e.getMessage());
            }
        }

        // 9. 销毁 session，强制重新登录（改密踢下线 -> 标记离线）
        userService.updateLoginStatus(user.getId(), 1);
        if (session != null) {
            session.invalidate();
        }
        // 清除 Cookie
        if (AuthInterceptor.COOKIE_USER_ID != null) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(AuthInterceptor.COOKIE_USER_ID, null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        result.put("success", true);
        result.put("message", "密码修改成功，请重新登录");
        return result;
    }
}
