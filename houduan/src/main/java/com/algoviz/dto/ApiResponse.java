package com.algoviz.dto;

import com.algoviz.common.enums.ErrorCode;
import com.algoviz.common.vo.R;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台管理 / 前台通用响应体：{code, message, data}
 *
 * 历史遗留：原先后台接口广泛使用此类。为保持前后端契约不变，保留此类；
 * 新增代码推荐直接使用 {@link R}，并通过 {@link #fromR(R)} / {@link #toR()} 互转。
 */
@Schema(description = "通用接口响应封装（后台管理）")
public class ApiResponse<T> {

    @Schema(description = "状态码：200=成功；400/401/403/404/409/429/500 为 HTTP 语义；扩展 400000+ 与 ErrorCode 一致")
    private int code;

    @Schema(description = "提示消息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 成功 ====================

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> ApiResponse<T> successMessage(String message) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), message, null);
    }

    // ==================== 失败 ====================

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ErrorCode.INTERNAL_ERROR.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(errorCode.getCode(), customMessage, null);
    }

    // ==================== 与 R<T> 互转 ====================

    public static <T> ApiResponse<T> fromR(R<T> r) {
        return new ApiResponse<>(r.getCode(), r.getMessage(), r.getData());
    }

    public R<T> toR() {
        boolean ok = this.code == 200 || this.code == 0 || (this.code >= 200 && this.code < 400000);
        return new R<>(ok, this.code, this.message, this.data);
    }

    // ==================== getter / setter ====================

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
