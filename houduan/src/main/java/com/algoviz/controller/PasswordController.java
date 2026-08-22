package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import com.algoviz.common.util.PasswordEncoderUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "密码重置", description = "忘记密码、重置密码相关接口")
public class PasswordController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordController.class);
    private static final String RESET_TOKEN_KEY_PREFIX = "algoviz:password:reset:token:";
    private static final long TOKEN_EXPIRY_MINUTES = 30; // 30分钟过期
    private static final int TOKEN_LENGTH = 32; // token长度

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/forgot")
    @Operation(summary = "忘记密码", description = "用户输入邮箱，生成密码重置链接")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> body,
                                              jakarta.servlet.http.HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        String email = body.get("email");
        String captcha = body.get("captcha");

        // 无论邮箱是否存在，返回相同提示，防止账号枚举
        String defaultMessage = "如果该邮箱已注册，重置链接将在30分钟内有效。";

        // 1. 校验图形验证码
        if (captcha != null && !captcha.isEmpty()) {
            if (!CaptchaController.verifyCaptchaInSession(captcha, httpRequest)) {
                result.put("success", false);
                result.put("message", "验证码错误或已过期");
                return result;
            }
        } else {
            result.put("success", false);
            result.put("message", "请输入验证码");
            return result;
        }

        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入邮箱");
            return result;
        }

        // 2. 查询邮箱是否存在
        User user = userService.findByEmail(email.trim());
        if (user != null) {
            // 生成安全随机token
            SecureRandom secureRandom = new SecureRandom();
            byte[] tokenBytes = new byte[TOKEN_LENGTH];
            secureRandom.nextBytes(tokenBytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

            // 计算token哈希（存储哈希而不是明文）
            String tokenHash = hashToken(token);

            // 存储到Redis：userId、过期时间、是否已使用
            String redisKey = RESET_TOKEN_KEY_PREFIX + tokenHash;
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("userId", String.valueOf(user.getId()));
            tokenData.put("expireAt", String.valueOf(System.currentTimeMillis() + TOKEN_EXPIRY_MINUTES * 60 * 1000));
            tokenData.put("used", "false");
            
            for (Map.Entry<String, String> entry : tokenData.entrySet()) {
                redisTemplate.opsForHash().put(redisKey, entry.getKey(), entry.getValue());
            }
            redisTemplate.expire(redisKey, TOKEN_EXPIRY_MINUTES, TimeUnit.MINUTES);

            // 构建重置链接
            String resetUrl = "http://localhost:5500/AlgoVize/qianduan/pages/reset-password.html?token=" + token;

            // 打印到控制台（开发者模式）
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║            🔑 密码重置链接（开发者模式）                  ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  用户邮箱  : " + padRight(email.trim(), 45) + "║");
            System.out.println("║  用户ID    : " + padRight(String.valueOf(user.getId()), 45) + "║");
            System.out.println("║  重置链接  : " + padRight(resetUrl, 45) + "║");
            System.out.println("║  有效期    : " + padRight(TOKEN_EXPIRY_MINUTES + " 分钟", 45) + "║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();

            logger.info("密码重置链接已生成 - 邮箱: {}, 用户ID: {}", email, user.getId());

            // 返回重置链接给前端（开发模式）
            result.put("resetUrl", resetUrl);
        }

        // 返回相同提示，防止账号枚举
        result.put("success", true);
        result.put("message", defaultMessage);
        return result;
    }

    @PostMapping("/do-reset")
    @Operation(summary = "执行密码重置", description = "使用token重置密码")
    public Map<String, Object> doResetPassword(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        // 1. 参数校验
        if (token == null || token.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "无效的重置链接");
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
        if (!newPassword.equals(confirmPassword)) {
            result.put("success", false);
            result.put("message", "两次输入的密码不一致");
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

        // 2. 校验token
        String tokenHash = hashToken(token);
        String redisKey = RESET_TOKEN_KEY_PREFIX + tokenHash;
        
        Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(redisKey);
        if (tokenData == null || tokenData.isEmpty()) {
            result.put("success", false);
            result.put("message", "重置链接无效或已过期");
            return result;
        }

        // 3. 检查是否已使用
        String used = (String) tokenData.get("used");
        if ("true".equals(used)) {
            result.put("success", false);
            result.put("message", "重置链接已使用");
            return result;
        }

        // 4. 检查是否过期
        String expireAt = (String) tokenData.get("expireAt");
        if (expireAt != null && Long.parseLong(expireAt) < System.currentTimeMillis()) {
            redisTemplate.delete(redisKey);
            result.put("success", false);
            result.put("message", "重置链接已过期");
            return result;
        }

        // 5. 获取用户ID
        String userId = (String) tokenData.get("userId");
        User user = userService.findById(Integer.parseInt(userId));
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 6. 更新密码
        String encodedPassword = PasswordEncoderUtil.md5Encode(newPassword);
        userService.updatePassword(user.getId(), encodedPassword);

        // 7. 标记token为已使用
        redisTemplate.opsForHash().put(redisKey, "used", "true");
        redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);

        // 8. 打印日志
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║            ✅ 密码重置成功                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  用户ID    : " + padRight(String.valueOf(user.getId()), 45) + "║");
        System.out.println("║  用户名    : " + padRight(user.getUsername(), 45) + "║");
        System.out.println("║  重置时间  : " + padRight(LocalDateTime.now().toString().replace("T", " "), 45) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        logger.info("密码重置成功 - 用户ID: {}, 用户名: {}", user.getId(), user.getUsername());

        result.put("success", true);
        result.put("message", "密码重置成功");
        return result;
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Token哈希计算失败", e);
        }
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        return s.length() >= n ? s.substring(0, n - 1) + "…" : s + " ".repeat(n - s.length());
    }

    @PostMapping("/test-generate-token")
    @Operation(summary = "测试用：生成重置token", description = "临时测试接口，生成token并存入Redis")
    public Map<String, Object> testGenerateToken(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String userId = body.get("userId");
        if (userId == null) userId = "530030";

        // 生成token
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // 计算token哈希
        String tokenHash = hashToken(token);

        // 存储到Redis
        String redisKey = RESET_TOKEN_KEY_PREFIX + tokenHash;
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("userId", userId);
        tokenData.put("expireAt", String.valueOf(System.currentTimeMillis() + TOKEN_EXPIRY_MINUTES * 60 * 1000));
        tokenData.put("used", "false");

        for (Map.Entry<String, String> entry : tokenData.entrySet()) {
            redisTemplate.opsForHash().put(redisKey, entry.getKey(), entry.getValue());
        }
        redisTemplate.expire(redisKey, TOKEN_EXPIRY_MINUTES, TimeUnit.MINUTES);

        result.put("success", true);
        result.put("token", token);
        result.put("resetUrl", "http://localhost:5500/AlgoVize/qianduan/pages/reset-password.html?token=" + token);
        return result;
    }
}
