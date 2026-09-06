package com.algoviz.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 登录失败处理器：用户取消授权 / 授权码异常 / state 不匹配 / 平台错误等
 * 一律 302 回前台登录页并带 oauth_error 提示（避免暴露 Spring Security 默认错误页）。
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    /** 前台地址（application.yml: app.frontend-base-url，可含前端内容根路径前缀） */
    @Value("${app.frontend-base-url:http://localhost:5500}")
    private String frontendBaseUrl;

    /** 前台登录页相对路径（application.yml: app.frontend-login-path） */
    @Value("${app.frontend-login-path:/pages/login.html}")
    private String frontendLoginPath;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 登录失败: {}", exception.getMessage());
        response.sendRedirect(frontendBaseUrl + frontendLoginPath + "?oauth_error=1");
    }
}
