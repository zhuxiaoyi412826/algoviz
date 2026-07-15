package com.algoviz.service;

import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;

public interface LoginService {
    LoginResponse login(LoginRequest request);
    String generateVerificationCode();
    boolean validateVerificationCode(String code);
    boolean verifyCodeFromWechat(String code, String openId);
    LoginResponse checkLoginStatus(String code);
}
