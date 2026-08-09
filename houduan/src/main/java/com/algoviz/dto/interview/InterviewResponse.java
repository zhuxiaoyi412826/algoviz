package com.algoviz.dto.interview;

import com.algoviz.dto.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 面试模块统一响应：补 success 字段 */
@Schema(description = "面试模块统一响应封装")
public class InterviewResponse<T> {

    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "数据")
    private T data;
    @Schema(description = "状态码")
    private int code;

    public InterviewResponse() {}

    public InterviewResponse(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public static <T> InterviewResponse<T> ok() {
        return new InterviewResponse<>(true, "操作成功", null, 200);
    }

    public static <T> InterviewResponse<T> ok(String message) {
        return new InterviewResponse<>(true, message, null, 200);
    }

    public static <T> InterviewResponse<T> ok(T data) {
        return new InterviewResponse<>(true, "操作成功", data, 200);
    }

    public static <T> InterviewResponse<T> ok(String message, T data) {
        return new InterviewResponse<>(true, message, data, 200);
    }

    public static <T> InterviewResponse<T> fail(String message) {
        return new InterviewResponse<>(false, message, null, 500000);
    }

    public static <T> InterviewResponse<T> fail(int code, String message) {
        return new InterviewResponse<>(false, message, null, code);
    }

    /** 兼容：ApiResponse 转 InterviewResponse（保持 success=true） */
    public static <T> InterviewResponse<T> fromApi(ApiResponse<T> ar) {
        return new InterviewResponse<>(true, ar.getMessage(), ar.getData(), ar.getCode());
    }

    // getter/setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
}
