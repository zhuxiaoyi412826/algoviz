package com.algoviz.common.exception;

import com.algoviz.common.enums.ErrorCode;

/**
 * 未认证异常：用户未登录 / Token 失效 / 会话过期
 * 对应 HTTP 401
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
