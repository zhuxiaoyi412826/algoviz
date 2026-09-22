package com.algoviz.controller;

import com.algoviz.entity.BanAppeal;
import com.algoviz.entity.User;
import com.algoviz.mapper.BanAppealMapper;
import com.algoviz.service.EmailService;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 封禁申诉（公开接口，无需登录）。
 *
 * <p>被封禁账号（user.status=0）登录会被 AccountStatusService 拒绝，因此申诉入口放在登录页，
 * 本控制器所有接口均不加登录校验，只依赖「图形验证码 + 邮箱验证码」两道验证：
 * 图形验证码用于限制邮件发送频率，邮箱验证码用于确认申诉人是该账号注册邮箱的持有者。</p>
 *
 * <p>管理端查看/处理申诉见 {@link AdminBanAppealController}（/api/admin/ban-appeals）。</p>
 */
@RestController
@RequestMapping("/api/ban-appeal")
@Tag(name = "封禁申诉", description = "被封禁用户提交申诉（无需登录）")
public class BanAppealController {

    private static final Logger logger = LoggerFactory.getLogger(BanAppealController.class);

    /** 申诉理由长度限制 */
    private static final int REASON_MIN_LENGTH = 10;
    private static final int REASON_MAX_LENGTH = 1000;
    /** 本页图形验证码的独立 Session 槽位，避免与登录页账号/邮箱 Tab 的验证码互相覆盖 */
    private static final String CAPTCHA_TYPE = "banappeal";
    /** 第三方授权自动注册账号的占位邮箱后缀：这类账号没有真实邮箱，无法走邮箱验证 */
    private static final String OAUTH_PLACEHOLDER_EMAIL_SUFFIX = "@oauth.local";

    @Autowired
    private UserService userService;

    @Autowired
    private BanAppealMapper banAppealMapper;

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-code")
    @Operation(summary = "发送申诉邮箱验证码", description = "校验账号确实被封禁且邮箱与注册邮箱一致后，向该邮箱发送验证码")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String username = trim(body.get("username"));
        String email = trim(body.get("email"));
        String captcha = trim(body.get("captcha"));

        if (username.isEmpty()) {
            return fail(result, "请输入被封禁的账号");
        }
        if (email.isEmpty()) {
            return fail(result, "请输入邮箱");
        }
        if (!isValidEmail(email)) {
            return fail(result, "邮箱格式不正确");
        }
        if (captcha.isEmpty()) {
            return fail(result, "请输入图形验证码");
        }
        // 先做账号校验（不涉及发信），再校验图形验证码：
        // 图形验证码是一次性的，校验通过即被消耗。若先校验验证码，用户只是填错账号也会白白烧掉验证码，
        // 被迫重新输入图形验证码（前端也只能清空重填）。
        User user = userService.findByUsername(username);
        String accountError = validateAccount(user, email);
        if (accountError != null) {
            return fail(result, accountError);
        }

        // 图形验证码校验：避免被脚本无成本地触发邮件发送
        if (!CaptchaController.verifyCaptchaInSession(captcha, request, CAPTCHA_TYPE)) {
            return fail(result, "图形验证码错误或已过期");
        }

        long remaining = emailService.getSendIntervalRemaining(email);
        if (remaining > 0) {
            return fail(result, "请 " + remaining + " 秒后再发送");
        }

        if (!emailService.sendVerificationCode(email)) {
            return fail(result, "发送失败，请稍后重试");
        }
        logger.info("封禁申诉验证码已发送: username={}, email={}", username, email);
        result.put("success", true);
        result.put("message", "验证码已发送到邮箱");
        return result;
    }

    @PostMapping
    @Operation(summary = "提交封禁申诉", description = "校验邮箱验证码后提交申诉，同一账号存在待处理申诉时不允许重复提交")
    public Map<String, Object> submit(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String username = trim(body.get("username"));
        String email = trim(body.get("email"));
        String code = trim(body.get("code"));
        String reason = trim(body.get("reason"));

        if (username.isEmpty()) {
            return fail(result, "请输入被封禁的账号");
        }
        if (email.isEmpty() || !isValidEmail(email)) {
            return fail(result, "请输入正确的邮箱");
        }
        if (code.isEmpty()) {
            return fail(result, "请输入邮箱验证码");
        }
        if (reason.length() < REASON_MIN_LENGTH) {
            return fail(result, "申诉理由至少 " + REASON_MIN_LENGTH + " 个字");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            return fail(result, "申诉理由不能超过 " + REASON_MAX_LENGTH + " 个字");
        }

        User user = userService.findByUsername(username);
        String accountError = validateAccount(user, email);
        if (accountError != null) {
            return fail(result, accountError);
        }

        if (!emailService.verifyCode(email, code)) {
            return fail(result, "邮箱验证码错误或已过期");
        }

        // 同一账号只允许有一条待处理申诉，避免刷申诉淹没后台
        if (banAppealMapper.countPendingByUserId((long) user.getId()) > 0) {
            return fail(result, "该账号已有待处理的申诉，请耐心等待管理员处理");
        }

        BanAppeal appeal = new BanAppeal();
        appeal.setUserId((long) user.getId());
        appeal.setUsername(user.getUsername());
        appeal.setEmail(email);
        appeal.setReason(reason);
        appeal.setStatus(BanAppeal.STATUS_PENDING);
        banAppealMapper.insert(appeal);

        logger.info("收到封禁申诉: appealId={}, userId={}, username={}",
                appeal.getId(), user.getId(), user.getUsername());
        result.put("success", true);
        result.put("message", "申诉已提交，管理员处理后会邮件通知你");
        return result;
    }

    /**
     * 账号与邮箱校验（发送验证码、提交申诉两处共用，保证口径一致）。
     *
     * @return 不通过时返回给用户的提示；通过时返回 null
     */
    private String validateAccount(User user, String email) {
        if (user == null) {
            return "该账号不存在";
        }
        if (user.getStatus() == null || user.getStatus() != 0) {
            return "该账号当前未被封禁，无需申诉";
        }
        String regEmail = trim(user.getEmail());
        if (regEmail.isEmpty() || regEmail.toLowerCase().endsWith(OAUTH_PLACEHOLDER_EMAIL_SUFFIX)) {
            return "该账号未绑定可用邮箱，无法通过邮箱验证提交申诉，请联系管理员";
        }
        if (!regEmail.equalsIgnoreCase(email)) {
            return "邮箱与该账号的注册邮箱不一致";
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    }

    private static Map<String, Object> fail(Map<String, Object> result, String message) {
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}