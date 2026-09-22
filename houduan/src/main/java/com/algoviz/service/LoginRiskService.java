package com.algoviz.service;

import com.algoviz.common.constant.SecurityConstants;
import com.algoviz.common.util.IpLocationUtil;
import com.algoviz.entity.LoginLog;
import com.algoviz.entity.User;
import com.algoviz.mapper.LoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录风控服务（面向生产的统一收口）
 *
 * <p>在既有「账号维度失败锁定」({@link LoginLockService}) 之外补齐三件事：</p>
 * <ol>
 *   <li><b>IP 维度窗口限速</b>：同一 IP 在 {@link SecurityConstants#IP_FAIL_WINDOW_MINUTES} 分钟内
 *       失败达到 {@link SecurityConstants#IP_FAIL_THRESHOLD} 次即锁定该 IP，拦截后续登录与
 *       OAuth 回调（调用点：登录入口、OAuth 失败处理器；拦截点：OAuth 回调守卫过滤器）；</li>
 *   <li><b>登录日志</b>：复用既有 login_log 表（不新增表），记录前台用户登录成功/失败；</li>
 *   <li><b>异常登录提醒</b>：登录成功时用设备指纹判断是否为新设备/新网络，是则发提醒邮件
 *       （{provider}_{openId}@oauth.local 占位邮箱跳过，避免必然退信）。</li>
 * </ol>
 *
 * <p>所有风控动作都不得影响登录主流程：Redis / 邮件异常一律降级并只记日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRiskService {

    /** 登录日志时间格式（与 LoginController 口径一致，禁用 toString().substring） */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 第三方自动注册账号的占位邮箱后缀：不可投递，跳过提醒 */
    private static final String OAUTH_PLACEHOLDER_EMAIL_SUFFIX = "@oauth.local";

    private final StringRedisTemplate redis;
    private final LoginLogMapper loginLogMapper;
    private final IpLocationUtil ipLocationUtil;
    private final EmailService emailService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final LoginLockService loginLockService;

    /** IP 维度检查结果 */
    public static class IpGuard {
        /** 是否已被锁定 */
        public boolean blocked;
        /** 锁定到期毫秒时间戳（未锁定时为 0） */
        public long expireAtMs;
    }

    // ==================== IP 维度：失败计数 + 锁定 ====================

    /** 前置检查：该 IP 是否处于锁定状态 */
    public IpGuard checkIp(HttpServletRequest request) {
        return checkIp(clientIp(request));
    }

    public IpGuard checkIp(String ip) {
        IpGuard guard = new IpGuard();
        if (ip == null || ip.isEmpty()) {
            return guard;
        }
        try {
            String lockKey = SecurityConstants.IP_LOCK_KEY + ip;
            String expireVal = redis.opsForValue().get(lockKey);
            if (expireVal == null || expireVal.isEmpty()) {
                return guard;
            }
            long expireAt = Long.parseLong(expireVal);
            if (expireAt > System.currentTimeMillis()) {
                guard.blocked = true;
                guard.expireAtMs = expireAt;
                return guard;
            }
            redis.delete(lockKey);
        } catch (NumberFormatException ignored) {
            redis.delete(SecurityConstants.IP_LOCK_KEY + ip);
        } catch (Exception e) {
            // Redis 不可用时放行（fail-open），不因风控组件故障阻断登录
            log.warn("[LoginRisk] IP 锁定检查失败，放行本次登录: {}", e.getMessage());
        }
        return guard;
    }

    /** 登录失败：累加该 IP 的窗口内失败次数，达到阈值即锁定 */
    public void recordIpFailure(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        String failKey = SecurityConstants.IP_FAIL_COUNT_KEY + ip;
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, SecurityConstants.IP_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= SecurityConstants.IP_FAIL_THRESHOLD) {
            long expireAt = System.currentTimeMillis()
                    + TimeUnit.MINUTES.toMillis(SecurityConstants.IP_LOCK_MINUTES);
            redis.opsForValue().set(SecurityConstants.IP_LOCK_KEY + ip, String.valueOf(expireAt),
                    SecurityConstants.IP_LOCK_MINUTES, TimeUnit.MINUTES);
            log.warn("[LoginRisk] IP {} 在 {} 分钟内失败 {} 次，已锁定 {} 分钟",
                    ip, SecurityConstants.IP_FAIL_WINDOW_MINUTES, count, SecurityConstants.IP_LOCK_MINUTES);
        }
    }

    /** 登录成功：清空该 IP 的失败计数与锁定标记 */
    public void resetIpFailure(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        redis.delete(SecurityConstants.IP_FAIL_COUNT_KEY + ip);
        redis.delete(SecurityConstants.IP_LOCK_KEY + ip);
    }

    /** 给前端展示的 IP 锁定提示文案 */
    public String describeIpBlock(IpGuard guard) {
        long remaining = guard == null ? 0L : guard.expireAtMs;
        return "当前网络登录失败次数过多，请 " + loginLockService.formatRemaining(remaining) + " 后再试";
    }

    // ==================== 登录成功 / 失败统一收尾 ====================

    /**
     * 登录成功后调用：清 IP 计数 → 写登录日志 → 新设备提醒。
     * 覆盖「账号密码登录」与「第三方授权登录」两条入口。
     */
    public void onLoginSuccess(User user, HttpServletRequest request) {
        String ip = clientIp(request);
        String userAgent = request.getHeader("User-Agent");
        try {
            resetIpFailure(ip);
        } catch (Exception e) {
            log.warn("[LoginRisk] 清除 IP 失败计数失败: {}", e.getMessage());
        }
        saveLoginLog(user != null && user.getId() != null ? String.valueOf(user.getId()) : "0",
                user != null ? user.getUsername() : "", ip, userAgent, true, null);
        notifyIfNewDevice(user, ip, userAgent, request.getHeader("Accept-Language"));
    }

    /**
     * 登录失败后调用：写登录日志 → 累加 IP 维度失败计数。
     * @param userId 可为 null（用户不存在 / 第三方登录未解析出账号）
     */
    public void onLoginFailure(Integer userId, String username, String failReason, HttpServletRequest request) {
        String ip = clientIp(request);
        saveLoginLog(userId != null ? String.valueOf(userId) : "0",
                username != null ? username : "", ip, request.getHeader("User-Agent"), false, failReason);
        try {
            recordIpFailure(ip);
        } catch (Exception e) {
            log.warn("[LoginRisk] IP 失败计数失败: {}", e.getMessage());
        }
    }

    // ==================== 登录日志 ====================

    /** 写 login_log（异常只记日志，不影响登录主流程） */
    public void saveLoginLog(String userId, String username, String ip, String userAgent,
                             boolean success, String failReason) {
        try {
            LoginLog entry = new LoginLog();
            // 列宽 VARCHAR(32)：UUID 去横线后正好 32 位
            entry.setId(UUID.randomUUID().toString().replace("-", ""));
            // user_id / username / ip 均为 NOT NULL 列，统一兜底空串
            entry.setUserId(truncate(userId == null ? "0" : userId, 32));
            entry.setUsername(truncate(username == null ? "" : username, 50));
            entry.setIp(truncate(ip == null ? "" : ip, 64));
            entry.setDevice(truncate(userAgent, 200));
            entry.setLocation(truncate(ipLocationUtil.getLocationByIp(ip), 200));
            entry.setLoginTime(LocalDateTime.now());
            entry.setStatus(success ? "success" : "failed");
            entry.setFailReason(success ? null : truncate(failReason, 255));
            loginLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("[LoginRisk] 写入登录日志失败: {}", e.getMessage());
        }
    }

    // ==================== 新设备 / 异常 IP 邮件提醒 ====================

    /**
     * 新设备/新网络登录提醒：占位邮箱跳过、发信失败不影响登录。
     */
    private void notifyIfNewDevice(User user, String ip, String userAgent, String acceptLanguage) {
        try {
            if (user == null || user.getId() == null) {
                return;
            }
            String email = user.getEmail();
            if (email == null || email.isEmpty() || email.endsWith(OAUTH_PLACEHOLDER_EMAIL_SUFFIX)) {
                return;
            }
            DeviceFingerprintService.FingerprintResult fp =
                    deviceFingerprintService.recognize(user.getId(), userAgent, ip, acceptLanguage);
            if (!fp.newDevice) {
                return;
            }
            String content = "检测到你的账号在新设备或新网络环境登录：\n"
                    + "账号：" + user.getUsername() + "\n"
                    + "登录时间：" + LocalDateTime.now().format(DATETIME_FMT) + "\n"
                    + "登录 IP：" + ip + "\n"
                    + "登录地点：" + ipLocationUtil.getLocationByIp(ip) + "\n"
                    + "设备信息：" + (userAgent == null ? "未知" : userAgent) + "\n\n"
                    + "如非本人操作，请立即修改密码或联系管理员。";
            emailService.sendNotificationEmail(email, "新设备登录提醒", content);
        } catch (Exception e) {
            log.warn("[LoginRisk] 发送新设备登录提醒失败: {}", e.getMessage());
        }
    }

    // ==================== 请求信息提取 ====================

    /** 真实客户端 IP：X-Real-IP（nginx 注入，可信）→ X-Forwarded-For → remoteAddr */
    public String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}