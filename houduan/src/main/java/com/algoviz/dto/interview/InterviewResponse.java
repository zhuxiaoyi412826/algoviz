package com.algoviz.dto.interview;

import com.algoviz.common.enums.ErrorCode;
import com.algoviz.common.vo.R;
import com.algoviz.dto.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 面试模块统一响应：{success, message, data, code}
 *
 * 为保持前端既有契约（面试模块 / 部分 OJ 模块前端依赖 success 布尔字段），保留此类。
 * 新增代码优先使用 {@link R}，通过 {@link #fromR(R)} / {@link #toR()} 互转。
 */
@Schema(description = "面试模块统一响应封装")
public class InterviewResponse<T> {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "提示消息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "状态码：200=成功；400000+ 与 ErrorCode 枚举对齐")
    private int code;

    public InterviewResponse() {}

    public InterviewResponse(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    // ==================== 成功 ====================

    public static <T> InterviewResponse<T> ok() {
        return new InterviewResponse<>(true, ErrorCode.SUCCESS.getMessage(), null, ErrorCode.SUCCESS.getCode());
    }

    public static <T> InterviewResponse<T> ok(String message) {
        return new InterviewResponse<>(true, message, null, ErrorCode.SUCCESS.getCode());
    }

    public static <T> InterviewResponse<T> ok(T data) {
        return new InterviewResponse<>(true, ErrorCode.SUCCESS.getMessage(), data, ErrorCode.SUCCESS.getCode());
    }

    public static <T> InterviewResponse<T> ok(String message, T data) {
        return new InterviewResponse<>(true, message, data, ErrorCode.SUCCESS.getCode());
    }

    // ==================== 失败 ====================

    public static <T> InterviewResponse<T> fail(String message) {
        return new InterviewResponse<>(false, message, null, ErrorCode.INTERNAL_ERROR.getCode());
    }

    public static <T> InterviewResponse<T> fail(int code, String message) {
        return new InterviewResponse<>(false, message, null, code);
    }

    public static <T> InterviewResponse<T> fail(ErrorCode errorCode) {
        return new InterviewResponse<>(false, errorCode.getMessage(), null, errorCode.getCode());
    }

    public static <T> InterviewResponse<T> fail(ErrorCode errorCode, String customMessage) {
        return new InterviewResponse<>(false, customMessage, null, errorCode.getCode());
    }

    // ==================== 与 R / ApiResponse 互转 ====================

    public static <T> InterviewResponse<T> fromR(R<T> r) {
        return new InterviewResponse<>(r.isSuccess(), r.getMessage(), r.getData(), r.getCode());
    }

    public R<T> toR() {
        return new R<>(this.success, this.code, this.message, this.data);
    }

    /** 兼容：ApiResponse 转 InterviewResponse（按 code 推导 success） */
    public static <T> InterviewResponse<T> fromApi(ApiResponse<T> ar) {
        boolean ok = ar != null
                && (ar.getCode() == 200 || ar.getCode() == 0 || (ar.getCode() >= 200 && ar.getCode() < 400000));
        return new InterviewResponse<>(ok,
                ar == null ? "" : ar.getMessage(),
                ar == null ? null : ar.getData(),
                ar == null ? 500 : ar.getCode());
    }

    // ==================== getter / setter ====================

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
}
