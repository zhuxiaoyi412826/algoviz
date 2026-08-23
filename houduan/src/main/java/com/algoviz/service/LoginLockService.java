package com.algoviz.service;

import com.algoviz.common.constant.SecurityConstants;
import com.algoviz.common.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败锁定服务
 * 分级策略：
 *   阶段1：连续失败 5 次 → 锁定 60 分钟
 *   阶段2：24小时窗口内累计失败 10 次 → 锁定 24 小时
 * 锁定键优先判断：只要处于锁定状态即拦截登录；解锁后失败计数仍保留至窗口自然过期或成功登录后清零。
 *
 * 计数 scope：
 *   - 前台普通用户：LoginLockType.USER，identifier 统一使用 username / email 解析到的唯一用户名
 *   - 后台管理员：LoginLockType.ADMIN，identifier 使用 admin username
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLockService {

    private final StringRedisTemplate redis;

    /** 计数类型 */
    public enum LoginLockType {
        USER(SecurityConstants.USER_FAIL_COUNT_KEY, SecurityConstants.USER_LOCK_KEY),
        ADMIN(SecurityConstants.ADMIN_FAIL_COUNT_KEY, SecurityConstants.ADMIN_LOCK_KEY);

        final String failPrefix;
        final String lockPrefix;
        LoginLockType(String failPrefix, String lockPrefix) {
            this.failPrefix = failPrefix;
            this.lockPrefix = lockPrefix;
        }
    }

    /** 锁定结果 */
    public static class LockStatus {
        /** 是否锁定 */
        public boolean locked;
        /** 锁定到期时间毫秒（null=未锁定） */
        public Long expireAtMs;
        /** 错误码（未锁定时为 null） */
        public ErrorCode errorCode;

        public static LockStatus unlocked() { return new LockStatus(); }
        public static LockStatus locked(long expireAtMs, ErrorCode code) {
            LockStatus s = new LockStatus();
            s.locked = true;
            s.expireAtMs = expireAtMs;
            s.errorCode = code;
            return s;
        }
    }

    // ==================== 对外接口 ====================

    /**
     * 前置检查：登录前先判断账号是否处于锁定状态
     * @return 若锁定返回 LockStatus.locked(...)，否则 unlocked()
     */
    public LockStatus checkLock(LoginLockType type, String identifier) {
        String lockKey = type.lockPrefix + identifier;
        String expireVal = redis.opsForValue().get(lockKey);
        if (expireVal != null && !expireVal.isEmpty()) {
            try {
                long expireAt = Long.parseLong(expireVal);
                if (expireAt > System.currentTimeMillis()) {
                    // 按锁定级别判断应该回哪个错误码：
                    // 若 key 的剩余 TTL > 阶段1时长 + 1小时 余量，则判定为阶段2长锁定
                    Long ttl = redis.getExpire(lockKey, TimeUnit.MINUTES);
                    ErrorCode code = (ttl != null && ttl > SecurityConstants.STAGE1_LOCK_MINUTES + 30)
                            ? ErrorCode.ACCOUNT_LOCKED_LONG
                            : ErrorCode.ACCOUNT_LOCKED_TEMP;
                    return LockStatus.locked(expireAt, code);
                }
                // 已过期：删除 key 保持干净
                redis.delete(lockKey);
            } catch (NumberFormatException ignored) {
                redis.delete(lockKey);
            }
        }
        return LockStatus.unlocked();
    }

    /**
     * 登录失败：累加失败计数；若达到阈值则写入锁定键
     */
    public void recordFailure(LoginLockType type, String identifier) {
        String failKey = type.failPrefix + identifier;
        String lockKey = type.lockPrefix + identifier;

        Long newCount = redis.opsForValue().increment(failKey);
        if (newCount != null && newCount == 1L) {
            // 首次设置：窗口 24 小时
            redis.expire(failKey, SecurityConstants.FAIL_COUNT_WINDOW_HOURS, TimeUnit.HOURS);
        }

        int count = newCount != null ? newCount.intValue() : 1;

        // 阶段2：累计 >=10 → 锁 24h
        if (count >= SecurityConstants.STAGE2_FAIL_THRESHOLD) {
            long expireAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(SecurityConstants.STAGE2_LOCK_HOURS);
            redis.opsForValue().set(lockKey, String.valueOf(expireAt),
                    SecurityConstants.STAGE2_LOCK_HOURS, TimeUnit.HOURS);
            log.warn("[LoginLock] 账号 {} 达到阶段2阈值（累计失败{}次），锁定 24h", identifier, count);
            return;
        }

        // 阶段1：连续 >=5 → 锁 60min
        // 注意：阶段1是"在未进入24h锁定之前，累计到5次也触发60分钟锁定"
        if (count >= SecurityConstants.STAGE1_FAIL_THRESHOLD) {
            long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(SecurityConstants.STAGE1_LOCK_MINUTES);
            // 如果已经存在阶段2锁定则覆盖（阶段2优先级更高，TTL 更长）
            Boolean hasStage2 = Optional.ofNullable(redis.getExpire(lockKey, TimeUnit.MINUTES))
                    .map(t -> t > SecurityConstants.STAGE1_LOCK_MINUTES + 30).orElse(false);
            if (!hasStage2) {
                redis.opsForValue().set(lockKey, String.valueOf(expireAt),
                        SecurityConstants.STAGE1_LOCK_MINUTES, TimeUnit.MINUTES);
                log.warn("[LoginLock] 账号 {} 达到阶段1阈值（累计失败{}次），锁定 60min", identifier, count);
            }
        }
    }

    /**
     * 登录成功：清空失败计数和锁定键
     */
    public void reset(LoginLockType type, String identifier) {
        String failKey = type.failPrefix + identifier;
        String lockKey = type.lockPrefix + identifier;
        redis.delete(failKey);
        redis.delete(lockKey);
    }

    /** 返回人类可读的剩余锁定时间字符串（给前端提示用） */
    public String formatRemaining(long expireAtMs) {
        long left = expireAtMs - System.currentTimeMillis();
        if (left <= 0) return "0分钟";
        long hours = left / 3_600_000L;
        long mins = (left % 3_600_000L) / 60_000L;
        long secs = (left % 60_000L) / 1_000L;
        if (hours > 0) return hours + "小时" + mins + "分钟";
        if (mins > 0) return mins + "分" + secs + "秒";
        return secs + "秒";
    }
}
