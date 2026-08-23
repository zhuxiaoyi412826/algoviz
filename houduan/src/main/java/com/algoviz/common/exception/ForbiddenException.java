package com.algoviz.common.exception;

import com.algoviz.common.enums.ErrorCode;

/**
 * 权限不足异常：已登录但角色/权限不允许访问
 * 对应 HTTP 403
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN.getCode(), message);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
