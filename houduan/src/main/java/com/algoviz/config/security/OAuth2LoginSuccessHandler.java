package com.algoviz.config.security;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.User;
import com.algoviz.service.OauthLoginService;
import com.algoviz.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 登录成功处理器
 *
 * <p>职责：OAuth 回调换取到第三方用户信息后——</p>
 * <ol>
 *   <li>解析平台(provider)、第三方 openId 及资料（昵称/头像）；</li>
 *   <li>调 {@link OauthLoginService}：命中 user_oauth 绑定直接登录，未命中自动注册并绑定；</li>
 *   <li>建立与「账号密码登录」完全一致的登录态：写 Session(LOGIN_USER)、写 Cookie(ALGOVIZ_UID)、置在线、刷最后登录时间；</li>
 *   <li>302 回跳前台个人中心（成功）/ 登录页（失败，带 oauth_error）。</li>
 * </ol>
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Autowired
    private OauthLoginService oauthLoginService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /** 前台地址（application.yml: app.frontend-base-url，可含前端内容根路径前缀，如 http://localhost:5500/AlgoVize/qianduan） */
    @Value("${app.frontend-base-url:http://localhost:5500}")
    private String frontendBaseUrl;

    /** 前台登录页相对路径（application.yml: app.frontend-login-path） */
    @Value("${app.frontend-login-path:/pages/login.html}")
    private String frontendLoginPath;

    /** 前台 OAuth 登录成功中转页（相对路径）：写入 localStorage 登录态后跳转 index.html */
    @Value("${app.frontend-callback-path:/oauth-callback.html}")
    private String frontendCallbackPath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String provider = null;
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            provider = token.getAuthorizedClientRegistrationId();

            OAuth2User principal = token.getPrincipal();
            Map<String, Object> attrs = principal.getAttributes();

            String openId = asText(attrs.get("id"));
            String login = asText(attrs.get("login"));
            String nickname = asText(attrs.get("name"));
            String avatar = asText(attrs.get("avatar_url"));
            // OAuth 原始返回 JSON 快照（落 user_oauth.raw_profile，便于调试与后续扩展）
            String rawProfile = null;
            try {
                rawProfile = objectMapper.writeValueAsString(attrs);
            } catch (Exception ignored) {
            }

            User user = oauthLoginService.loginOrRegister(provider, openId, login, nickname, avatar, rawProfile);

            // 与 LoginController 账号密码登录完全一致的登录态建立
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthInterceptor.SESSION_USER, user);
            AuthInterceptor.setCookie(response, AuthInterceptor.COOKIE_USER_ID,
                    String.valueOf(user.getId()), AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);
            userService.updateLoginStatus(user.getId(), 0);   // 置在线
            userService.updateLastLogin(user.getId());        // 写 user_visit_stat.last_login_at（复用既有口径）

            log.info("OAuth2 登录成功: provider={}, openId={}, username={}, userId={}",
                    provider, openId, user.getUsername(), user.getId());

            // 回跳前端中转页：先写 localStorage 登录态，再落到站点首页 index.html（避免直跳 profile 因
            // 前端登录态缺失被弹回 login.html；Cookie Secure 在纯 HTTP 本地可能不被浏览器存储，Session 兜底）
            response.sendRedirect(frontendBaseUrl + frontendCallbackPath + "?id=" + user.getId());
        } catch (Exception e) {
            // 账号注销/封禁、绑定异常、注册失败等 → 回到登录页提示，不让错误页裸奔。
            // 只向前端暴露友好原因，不把原始 SQL/堆栈细节带进 URL 与页面（防信息泄露）
            log.warn("OAuth2 登录处理失败: provider={}, error={}", provider, e.toString());
            String friendly = (e instanceof BusinessException) ? e.getMessage() : "服务异常，请稍后重试或使用其他方式登录";
            response.sendRedirect(frontendBaseUrl + frontendLoginPath
                    + "?oauth_error=" + urlEncode(friendly));
        }
    }

    private static String asText(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** 简单 URL 编码，避免错误信息含空格/中文破坏 query */
    private static String urlEncode(String s) {
        if (s == null || s.isEmpty()) {
            return "1";
        }
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "1";
        }
    }
}
