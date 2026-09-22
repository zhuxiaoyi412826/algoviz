package com.algoviz.common.constant;

/**
 * 安全相关常量：Redis key 前缀 / 阈值 / 锁定时长
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    // ==================== 登录失败锁定 ====================

    /** 第一阶段：连续失败次数阈值 → 锁定60分钟 */
    public static final int STAGE1_FAIL_THRESHOLD = 5;
    /** 第二阶段：累计失败次数阈值 → 锁定24小时 */
    public static final int STAGE2_FAIL_THRESHOLD = 10;

    /** 第一阶段锁定时长（分钟） */
    public static final int STAGE1_LOCK_MINUTES = 60;
    /** 第二阶段锁定时长（小时） */
    public static final int STAGE2_LOCK_HOURS = 24;

    /** 失败次数统计的窗口（小时）：超过此时间后计数自然过期 */
    public static final int FAIL_COUNT_WINDOW_HOURS = 24;

    /** 前台用户登录失败次数 key (algoviz:login:fail:count:user:{identifier}) */
    public static final String USER_FAIL_COUNT_KEY = "algoviz:login:fail:count:user:";
    /** 前台用户锁定标记 key (algoviz:login:fail:lock:user:{identifier}) → value=锁定到期毫秒时间戳 */
    public static final String USER_LOCK_KEY = "algoviz:login:fail:lock:user:";

    /** 后台管理员登录失败次数 key (algoviz:login:fail:count:admin:{username}) */
    public static final String ADMIN_FAIL_COUNT_KEY = "algoviz:login:fail:count:admin:";
    /** 后台管理员锁定标记 key */
    public static final String ADMIN_LOCK_KEY = "algoviz:login:fail:lock:admin:";

    // ==================== Email验证码 ====================

    /** 邮箱验证码 Redis key 前缀：algoviz:email:code:{email} */
    public static final String EMAIL_CODE_KEY = "algoviz:email:code:";
    /** 邮箱验证码发送频率限制 Redis key 前缀 */
    public static final String EMAIL_SEND_LIMIT_KEY = "algoviz:email:send:limit:";

    // ==================== 登录风控：IP 维度失败锁定 ====================
    // 与账号维度（LoginLockService）并列：账号维度防撞库，IP 维度防单机批量尝试（含 OAuth 回调）

    /** IP 失败次数 key 前缀 (algoviz:login:fail:count:ip:{ip}) */
    public static final String IP_FAIL_COUNT_KEY = "algoviz:login:fail:count:ip:";
    /** IP 锁定标记 key 前缀 (algoviz:login:fail:lock:ip:{ip}) → value=锁定到期毫秒时间戳 */
    public static final String IP_LOCK_KEY = "algoviz:login:fail:lock:ip:";
    /** IP 失败计数窗口（分钟）：窗口内计数达到阈值即锁定 */
    public static final int IP_FAIL_WINDOW_MINUTES = 10;
    /** IP 维度阈值：窗口内失败次数达到该值锁定（比账号维度宽松，避免误伤 NAT 出口） */
    public static final int IP_FAIL_THRESHOLD = 20;
    /** IP 锁定时长（分钟） */
    public static final int IP_LOCK_MINUTES = 30;

    // ==================== 登录风控：设备指纹 ====================

    /** 设备指纹 key 前缀：algoviz:login:device:{userId}:{fingerprint} */
    public static final String DEVICE_FINGERPRINT_KEY = "algoviz:login:device:";
    /** 设备指纹记忆时长（天）：期间内同一 UA+IP段+语言 视为同一设备，不再重复提醒 */
    public static final int DEVICE_FINGERPRINT_TTL_DAYS = 30;
}
