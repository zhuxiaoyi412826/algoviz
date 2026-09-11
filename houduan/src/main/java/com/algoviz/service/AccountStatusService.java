package com.algoviz.service;

import com.algoviz.entity.User;
import com.algoviz.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 账号登录状态统一校验（账号密码 / 邮箱验证码 / 微信扫码 / 第三方 OAuth / 拦截器自动登录共用）。
 *
 * 注销 15 天冷静期规则：
 *  - status=-1 且 cancel_at 在 {@link #CANCEL_COOLING_DAYS} 天内 → 冷静期：
 *      本次登录自动撤销注销（status 恢复 1、cancel_at 清空），放行；
 *  - status=-1 且 cancel_at 已超期，或 cancel_at 为 NULL（历史注销账号）→ 永久注销，拒绝登录；
 *  - status=0 封禁 → 拒绝。
 */
@Component
public class AccountStatusService {

    private static final Logger logger = LoggerFactory.getLogger(AccountStatusService.class);

    /** 注销冷静期天数 */
    public static final int CANCEL_COOLING_DAYS = 15;

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录前校验。调用方传入的 user 须为按主键/账号查到的最新对象；
     * 若命中冷静期，本方法会执行撤销并同步修改该内存对象的 status/cancelAt，调用方可直接继续登录流程。
     *
     * @return 校验结果（allowed=true 放行；false 时取 rejectReason 提示用户）
     */
    public LoginCheck checkForLogin(User user) {
        if (user == null) {
            return LoginCheck.reject("账号不存在，请重新登录");
        }
        Integer st = user.getStatus();
        if (st != null && st == 0) {
            return LoginCheck.reject("账号已被禁用，请联系管理员");
        }
        if (st != null && st == -1) {
            LocalDateTime cancelAt = user.getCancelAt();
            if (cancelAt != null && LocalDateTime.now().isBefore(cancelAt.plusDays(CANCEL_COOLING_DAYS))) {
                // 冷静期内：本次登录视为主动撤销注销
                int rows = userMapper.revokeCancellation(user.getId());
                if (rows > 0) {
                    user.setStatus(1);
                    user.setCancelAt(null);
                    logger.warn("注销冷静期内登录，自动撤销注销: userId={}, username={}, 申请时间={}",
                            user.getId(), user.getUsername(), cancelAt);
                    return LoginCheck.revoked();
                }
                // 并发兜底：撤销可能已被同一账号的另一个登录请求抢先完成（多端/双击）。
                // 重查库确认：若已恢复正常则直接放行（撤销提示由抢先完成的那次登录展示）；
                // 仅在状态仍异常时拒绝。
                User fresh = userMapper.findById(user.getId());
                if (fresh != null && fresh.getStatus() != null && fresh.getStatus() == 1) {
                    user.setStatus(1);
                    user.setCancelAt(null);
                    logger.info("注销撤销已被并发登录抢先完成，本次直接放行: userId={}, username={}",
                            user.getId(), user.getUsername());
                    return LoginCheck.allowed();
                }
                return LoginCheck.reject("账号状态异常，请刷新后重试");
            }
            return LoginCheck.reject("账号已注销");
        }
        return LoginCheck.allowed();
    }

    /** 登录校验结果 */
    public static class LoginCheck {
        /** 是否放行 */
        private final boolean allowed;
        /** 拒绝原因（allowed=false 时有值） */
        private final String rejectReason;
        /** 本次登录是否自动撤销了注销申请（用于登录成功提示） */
        private final boolean cancellationRevoked;

        private LoginCheck(boolean allowed, String rejectReason, boolean cancellationRevoked) {
            this.allowed = allowed;
            this.rejectReason = rejectReason;
            this.cancellationRevoked = cancellationRevoked;
        }

        static LoginCheck allowed() {
            return new LoginCheck(true, null, false);
        }

        static LoginCheck revoked() {
            return new LoginCheck(true, null, true);
        }

        static LoginCheck reject(String reason) {
            return new LoginCheck(false, reason, false);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public boolean isCancellationRevoked() {
            return cancellationRevoked;
        }
    }
}
