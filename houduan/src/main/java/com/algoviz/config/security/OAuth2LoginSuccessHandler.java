package com.algoviz.config.security;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.common.exception.BusinessException;
import com.algoviz.controller.UserOauthBindingController;
import com.algoviz.entity.User;
import com.algoviz.service.LoginLockService;
import com.algoviz.service.LoginRiskService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 登录成功处理器
 *
 * <p>职责：OAuth 回调换取到第三方用户信息后——</p>
 * <ol>
 *   <li>解析平台(provider)、第三方 openId 及资料（昵称/头像）；</li>
 *   <li>判断 Session 是否存在「绑定意图」：
 *       <ul>
 *         <li>是 → 将第三方账号绑定到当前登录用户（bind_scene=2），回跳个人中心带绑定结果；</li>
 *         <li>否 → 走原登录流程：命中 user_oauth 绑定直接登录，未命中自动注册并绑定。</li>
 *       </ul></li>
 *   <li>登录场景下建立与「账号密码登录」完全一致的登录态；</li>
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

    /** 账号维度失败锁定（与密码登录共用阈值与键） */
    @Autowired
    private LoginLockService loginLockService;

    /** 登录风控：IP 维度计数、登录日志、新设备提醒 */
    @Autowired
    private LoginRiskService loginRiskService;

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

    /** 前台个人中心相对路径（绑定成功后回跳） */
    @Value("${app.frontend-profile-path:/pages/profile.html}")
    private String frontendProfilePath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String provider = null;
        HttpSession session = request.getSession(false);
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

            // ---- 绑定模式：Session 中存在绑定意图 → 绑定到当前登录用户 ----
            Object bindIntent = session != null ? session.getAttribute(UserOauthBindingController.SESSION_BIND_INTENT) : null;
            if (bindIntent instanceof String intentProvider && intentProvider.equalsIgnoreCase(provider)) {
                handleBind(request, response, session, provider, openId, nickname, avatar, rawProfile);
                return;
            }

            // ---- 登录模式：命中绑定直接登录，未命中自动注册 ----
            User user = oauthLoginService.loginOrRegister(provider, openId, login, nickname, avatar, rawProfile);

            // 账号维度风控：密码登录锁定的账号，不允许用第三方授权绕过（阈值复用 LoginLockService）
            LoginLockService.LockStatus lockStatus =
                    loginLockService.checkLock(LoginLockService.LoginLockType.USER, user.getUsername());
            if (lockStatus.locked) {
                loginRiskService.onLoginFailure(user.getId(), user.getUsername(),
                        "账号已锁定（第三方登录）", request);
                response.sendRedirect(frontendBaseUrl + frontendLoginPath + "?oauth_error=" + urlEncode(
                        "登录失败次数过多，账号已锁定，剩余 " + loginLockService.formatRemaining(lockStatus.expireAtMs)));
                return;
            }

            // 与 LoginController 账号密码登录完全一致的登录态建立
            HttpSession loginSession = request.getSession(true);
            loginSession.setAttribute(AuthInterceptor.SESSION_USER, user);
            AuthInterceptor.setCookie(request, response, AuthInterceptor.COOKIE_USER_ID,
                    String.valueOf(user.getId()), AuthInterceptor.COOKIE_MAX_AGE_DAYS_4);
            userService.updateLoginStatus(user.getId(), 0);   // 置在线
            userService.updateLastLogin(user.getId());        // 写 user_visit_stat.last_login_at（复用既有口径）

            // 登录成功：清账号失败计数 + 清 IP 失败计数 + 写 login_log + 新设备/新网络提醒
            loginLockService.reset(LoginLockService.LoginLockType.USER, user.getUsername());
            loginRiskService.onLoginSuccess(user, request);

            log.info("OAuth2 登录成功: provider={}, openId={}, username={}, userId={}",
                    provider, openId, user.getUsername(), user.getId());

            // 回跳前端中转页：先写 localStorage 登录态，再落到站点首页 index.html
            response.sendRedirect(frontendBaseUrl + frontendCallbackPath + "?id=" + user.getId());
        } catch (Exception e) {
            // 账号注销/封禁、绑定异常、注册失败等 → 回到登录页提示，不让错误页裸奔。
            log.warn("OAuth2 处理失败: provider={}, mode={}, error={}", provider,
                    bindIntentOf(session), e.toString());
            String friendly = (e instanceof BusinessException) ? e.getMessage() : "服务异常，请稍后重试或使用其他方式登录";
            // 绑定失败回个人中心；登录失败回登录页
            boolean bindMode = session != null && session.getAttribute(UserOauthBindingController.SESSION_BIND_INTENT) != null;
            if (!bindMode) {
                // 登录模式下的异常同样计入 IP 风控（绑定失败与登录失败无关，不计入）
                loginRiskService.onLoginFailure(null, provider, friendly, request);
            }
            String backTo = bindMode
                    ? frontendBaseUrl + frontendProfilePath + "?oauth_bind_error=" + urlEncode(friendly)
                    : frontendBaseUrl + frontendLoginPath + "?oauth_error=" + urlEncode(friendly);
            clearBindIntent(session);
            response.sendRedirect(backTo);
        }
    }

    /** 绑定模式：把第三方账号绑定到当前登录用户，然后回跳个人中心 */
    private void handleBind(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                            String provider, String openId, String nickname, String avatar, String rawProfile) throws IOException {
        User current = currentLoginUser(session, request);
        if (current == null || current.getId() == null) {
            clearBindIntent(session);
            response.sendRedirect(frontendBaseUrl + frontendProfilePath
                    + "?oauth_bind_error=" + urlEncode("登录态已失效，请重新登录后再绑定"));
            return;
        }
        oauthLoginService.bindToExistingAccount(current.getId(), provider, openId, nickname, avatar, rawProfile);
        clearBindIntent(session);
        log.info("OAuth2 手动绑定成功: userId={}, provider={}, openId={}", current.getId(), provider, openId);
        response.sendRedirect(frontendBaseUrl + frontendProfilePath + "?oauth_bind=success&provider=" + provider);
    }

    /** 取当前登录用户（Session 优先） */
    private User currentLoginUser(HttpSession session, HttpServletRequest request) {
        if (session != null) {
            Object attr = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (attr instanceof User u) {
                return u;
            }
        }
        String uid = AuthInterceptor.getCookieValue(request, AuthInterceptor.COOKIE_USER_ID);
        if (uid != null) {
            try {
                return userService.findById(Integer.parseInt(uid));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void clearBindIntent(HttpSession session) {
        if (session != null) {
            session.removeAttribute(UserOauthBindingController.SESSION_BIND_INTENT);
        }
    }

    private String bindIntentOf(HttpSession session) {
        if (session == null) {
            return "login";
        }
        Object v = session.getAttribute(UserOauthBindingController.SESSION_BIND_INTENT);
        return v != null ? "bind(" + v + ")" : "login";
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
