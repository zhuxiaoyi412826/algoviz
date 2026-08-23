package com.algoviz.common.enums;

import lombok.Getter;

/**
 * 全局错误码枚举
 * 编码规则：
 *   200    - 成功
 *   400xx  - 请求参数错误（客户端）
 *   401xx  - 未认证
 *   403xx  - 无权限
 *   404xx  - 资源不存在
 *   409xx  - 业务冲突（重复/状态不允许）
 *   429xx  - 限流
 *   500xx  - 服务端内部错误
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    // ===== 客户端参数错误 400xx =====
    BAD_REQUEST(400000, "请求参数错误"),
    VALIDATION_FAILED(400001, "参数校验失败"),
    MISSING_PARAMETER(400002, "缺少必填参数"),
    PARAM_TYPE_MISMATCH(400003, "参数类型不匹配"),

    // ===== 未认证 401xx =====
    UNAUTHORIZED(401000, "未登录或登录已过期"),
    LOGIN_FAILED(401001, "用户名或密码错误"),
    INVALID_TOKEN(401002, "Token 无效或已过期"),
    CAPTCHA_INVALID(401003, "图形验证码错误"),
    EMAIL_CODE_INVALID(401004, "邮箱验证码错误或已过期"),
    SESSION_EXPIRED(401005, "会话已过期，请重新登录"),
    ACCOUNT_LOCKED_TEMP(401006, "登录失败次数过多，账号已锁定，请稍后再试"),
    ACCOUNT_LOCKED_LONG(401007, "登录失败次数过多，账号已锁定24小时，请明天再试"),

    // ===== 无权限 403xx =====
    FORBIDDEN(403000, "无权限访问该资源"),
    ROLE_NOT_ALLOWED(403001, "当前角色不允许执行此操作"),
    RESOURCE_OWNER_REQUIRED(403002, "只能操作自己的资源"),

    // ===== 资源不存在 404xx =====
    NOT_FOUND(404000, "资源不存在"),
    USER_NOT_FOUND(404001, "用户不存在"),
    PROBLEM_NOT_FOUND(404002, "题目不存在"),
    SOLUTION_NOT_FOUND(404003, "题解不存在"),
    INTERVIEW_PROBLEM_NOT_FOUND(404004, "面试题不存在"),
    EMAIL_NOT_FOUND(404005, "邮箱未注册"),

    // ===== 业务冲突 409xx =====
    CONFLICT(409000, "业务状态冲突"),
    USERNAME_EXISTS(409001, "用户名已存在"),
    EMAIL_EXISTS(409002, "邮箱已注册"),
    PROBLEM_NO_EXISTS(409003, "题目编号已存在"),
    OLD_PASSWORD_WRONG(409004, "旧密码错误"),
    PASSWORD_SAME_AS_OLD(409005, "新密码不能与旧密码相同"),
    INSUFFICIENT_COINS(409006, "金币不足"),
    RESET_TOKEN_INVALID(409007, "重置链接无效或已过期"),
    ORDER_STATUS_INVALID(409008, "订单状态不允许此操作"),

    // ===== 限流 429xx =====
    TOO_MANY_REQUESTS(429000, "请求过于频繁，请稍后再试"),
    EMAIL_SEND_TOO_FREQUENT(429001, "验证码发送过于频繁，请等待60秒"),
    SUBMIT_TOO_FREQUENT(429002, "提交代码过于频繁，请稍后再试"),

    // ===== 文件上传 413xx =====
    FILE_TOO_LARGE(413001, "文件大小超过限制"),
    FILE_TYPE_NOT_ALLOWED(413002, "不支持的文件类型"),

    // ===== 服务端内部错误 500xx =====
    INTERNAL_ERROR(500000, "服务器内部错误"),
    EMAIL_SEND_FAILED(500001, "邮件发送失败"),
    SMS_SEND_FAILED(500002, "短信发送失败"),
    DATABASE_ERROR(500003, "数据库操作异常"),
    ES_UNAVAILABLE(500004, "搜索服务暂不可用"),
    REDIS_UNAVAILABLE(500005, "缓存服务暂不可用"),
    WECHAT_PAY_ERROR(500006, "微信支付异常"),
    EXPORT_ERROR(500007, "导出失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
