package com.algoviz.common.vo;

import com.algoviz.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目级统一返回体（全局任何接口推荐都返回此类型）。
 *
 * 设计原则：
 *   - code = 2xxxxx 表示成功（约定 SUCCESS.code = 200）
 *   - code >= 400000 表示业务失败（与 ErrorCode 枚举对齐）
 *   - success 字段直接由 code < 400000 自动推导，避免前端既要判断 code 又要判断 success
 *   - 兼容老接口：
 *       - ApiResponse<T>（后台管理 + 少数前台接口）: {code, message, data}
 *       - InterviewResponse<T>（面试题模块）: {success, message, data, code}
 *     通过 R.toApiResponse() / R.toInterviewResponse() 一键互转
 *
 * 使用：
 *   return R.ok(data);
 *   return R.ok("登录成功", tokenMap);
 *   return R.fail(ErrorCode.ACCOUNT_LOCKED_TEMP);
 *   return R.fail(409006, "金币不足");
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目统一返回体")
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否成功（code<400000 视为成功）")
    private boolean success;

    @Schema(description = "状态码：200=成功；400xx=参数错误；401xx=未认证；403xx=无权限；409xx=业务冲突；500xx=服务端异常")
    private int code;

    @Schema(description = "提示消息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    // ==================== 成功 ====================

    public static <T> R<T> ok() {
        return new R<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(true, ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> okMessage(String message) {
        return new R<>(true, ErrorCode.SUCCESS.getCode(), message, null);
    }

    // ==================== 失败 ====================

    public static <T> R<T> fail(String message) {
        return new R<>(false, ErrorCode.INTERNAL_ERROR.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(false, code, message, null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String customMessage) {
        return new R<>(false, errorCode.getCode(), customMessage, null);
    }

    // ==================== 构造/链式 setter（为了兼容无 lombok 环境场景） ====================

    public R<T> success(boolean success) { this.success = success; return this; }
    public R<T> code(int code) { this.code = code; return this; }
    public R<T> message(String message) { this.message = message; return this; }
    public R<T> data(T data) { this.data = data; return this; }

    // ==================== 向老 ApiResponse / InterviewResponse 转换 ====================

    public com.algoviz.dto.ApiResponse<T> toApiResponse() {
        return new com.algoviz.dto.ApiResponse<>(this.code, this.message, this.data);
    }

    public com.algoviz.dto.interview.InterviewResponse<T> toInterviewResponse() {
        return new com.algoviz.dto.interview.InterviewResponse<>(
                this.success, this.message, this.data, this.code);
    }

    // ==================== 从老类型构造 R ====================

    public static <T> R<T> fromApi(com.algoviz.dto.ApiResponse<T> ar) {
        boolean ok = ar != null && (ar.getCode() == 200 || ar.getCode() == 0 || (ar.getCode() < 400000 && ar.getCode() >= 200));
        return new R<>(ok, ar == null ? 500 : ar.getCode(), ar == null ? "" : ar.getMessage(), ar == null ? null : ar.getData());
    }

    public static <T> R<T> fromInterview(com.algoviz.dto.interview.InterviewResponse<T> ir) {
        return new R<>(ir == null || ir.isSuccess(),
                ir == null ? 500 : ir.getCode(),
                ir == null ? "" : ir.getMessage(),
                ir == null ? null : ir.getData());
    }
}
