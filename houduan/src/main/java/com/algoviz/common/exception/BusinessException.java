package com.algoviz.common.exception;

import com.algoviz.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类
 * Service 层遇到业务逻辑错误时直接抛出此异常，由 GlobalExceptionHandler 统一包装成 InterviewResponse 返回
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }
}
