package com.algoviz.config.security;

import com.algoviz.service.LoginRiskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 登录失败处理器：用户取消授权 / 授权码异常 / state 不匹配 / 平台错误等
 * 一律 302 回前台登录页并带 oauth_error 提示（避免暴露 Spring Security 默认错误页）。
 *
 * <p>同时作为 IP 维度风控的计数入口：每次失败累加该 IP 的窗口内失败次数，
 * 达到阈值后由 {@link OAuth2CallbackGuardFilter} 在回调入口直接拦截。</p>
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Autowired
    private LoginRiskService loginRiskService;

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
        log.warn("OAuth2 登录失败: ip={}, uri={}, error={}",
                loginRiskService.clientIp(request), request.getRequestURI(), exception.getMessage());
        // 用 registrationId（github / gitee）充当日志里的用户名占位，便于后台按平台筛选
        String registrationId = registrationIdOf(request);
        loginRiskService.onLoginFailure(null, registrationId, summary(exception), request);
        response.sendRedirect(frontendBaseUrl + frontendLoginPath + "?oauth_error=1");
    }

    /** 从回调路径 /login/oauth2/code/{registrationId} 解析平台标识 */
    private String registrationIdOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(OAuth2CallbackGuardFilter.CALLBACK_PATH_PREFIX)) {
            return "oauth";
        }
        String registrationId = uri.substring(OAuth2CallbackGuardFilter.CALLBACK_PATH_PREFIX.length());
        return registrationId.isEmpty() ? "oauth" : registrationId;
    }

    /** 失败原因入库前的压缩（列宽 255） */
    private String summary(AuthenticationException exception) {
        String message = exception.getMessage();
        return message == null || message.isEmpty() ? "第三方授权失败" : message;
    }
}
