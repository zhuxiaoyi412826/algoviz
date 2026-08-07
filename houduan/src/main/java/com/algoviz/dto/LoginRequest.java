package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登录请求")
@Data
public class LoginRequest {
    @Schema(description = "登录凭证/验证码")
    private String code;
    @Schema(description = "验证码")
    private String verificationCode;
}
