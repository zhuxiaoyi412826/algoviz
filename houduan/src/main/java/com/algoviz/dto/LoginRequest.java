package com.algoviz.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String code;
    private String verificationCode;
}
