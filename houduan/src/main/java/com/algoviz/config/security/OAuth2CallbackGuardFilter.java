package com.algoviz.config.security;

import com.algoviz.service.LoginRiskService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth 回调入口 IP 风控过滤器
 *
 * <p>挂在 {@code OAuth2LoginAuthenticationFilter} 之前：命中 {@code /login/oauth2/code/*} 时，
 * 先判断该 IP 是否因窗口内失败次数过多被锁定（计数由 {@link OAuth2LoginFailureHandler} 累加、
 * 成功登录后清零），命中则直接 302 回前台登录页并带 oauth_error，<b>不做授权码换取</b>，
 * 避免被锁定的来源继续消耗第三方平台的 token 端点与本地资源。</p>
 *
 * <p>只做「拦截」，不做「计数」：因此正常登录不会被自己限速。</p>
 */
@Component
public class OAuth2CallbackGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2CallbackGuardFilter.class);

    /** OAuth2 回调路径前缀（与 spring-security 的默认回调路径一致） */
    public static final String CALLBACK_PATH_PREFIX = "/login/oauth2/code/";

    @Autowired
    private LoginRiskService loginRiskService;

    /** 前台地址（application.yml: app.frontend-base-url，可含前端内容根路径前缀） */
    @Value("${app.frontend-base-url:http://localhost:5500}")
    private String frontendBaseUrl;

    /** 前台登录页相对路径（application.yml: app.frontend-login-path） */
    @Value("${app.frontend-login-path:/pages/login.html}")
    private String frontendLoginPath;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(CALLBACK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        LoginRiskService.IpGuard guard = loginRiskService.checkIp(request);
        if (guard.blocked) {
            log.warn("[OAuth2Guard] IP {} 已被锁定，拦截回调: {}", loginRiskService.clientIp(request), request.getRequestURI());
            String message = loginRiskService.describeIpBlock(guard);
            response.sendRedirect(frontendBaseUrl + frontendLoginPath
                    + "?oauth_error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }
}