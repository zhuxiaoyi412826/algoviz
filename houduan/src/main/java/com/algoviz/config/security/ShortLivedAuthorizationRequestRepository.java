package com.algoviz.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 带短时效的 OAuth2 授权请求仓储（state 加固）
 *
 * <p>默认实现 {@code HttpSessionOAuth2AuthorizationRequestRepository} 把授权请求（含 state、
 * PKCE 的 code_verifier）一直留在 Session 里，只要用户不回调就一直有效，可被重放。
 * 本实现仍存储在 HttpSession（登录态本就依赖 Session，无需引入额外组件），但为每条授权请求
 * 附带过期时间戳，超过 {@code app.oauth.authorization-request-ttl-minutes}（默认 5 分钟）后
 * {@link #loadAuthorizationRequest} 直接视为不存在 → 不匹配 state → 走 failureHandler 回登录页，
 * 从而把「state 有效窗口」从「整个会话」压缩到「几分钟」。</p>
 *
 * <p>同时在保存新授权请求时顺带清理 Session 中已过期的条目，避免用户反复点击登录导致
 * Session 里堆积大量僵尸 state。</p>
 */
public class ShortLivedAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(ShortLivedAuthorizationRequestRepository.class);

    /** Session 属性前缀（与 Spring 默认实现区分，避免两套命名混用） */
    private static final String ATTR_PREFIX = ShortLivedAuthorizationRequestRepository.class.getName() + ".REQUEST.";
    /** 过期时间戳属性后缀，值为 Long（epoch millis） */
    private static final String EXPIRES_SUFFIX = ".EXPIRES_AT";

    private final long ttlMillis;

    public ShortLivedAuthorizationRequestRepository(int ttlMinutes) {
        this.ttlMillis = TimeUnit.MINUTES.toMillis(Math.max(1, ttlMinutes));
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        HttpSession session = request.getSession(false);
        if (state == null || session == null) {
            return null;
        }
        Object authRequest = session.getAttribute(ATTR_PREFIX + state);
        Object expiresAt = session.getAttribute(ATTR_PREFIX + state + EXPIRES_SUFFIX);
        if (!(authRequest instanceof OAuth2AuthorizationRequest authorizationRequest)
                || !(expiresAt instanceof Long expires)) {
            return null;
        }
        if (System.currentTimeMillis() > expires) {
            log.warn("OAuth2 授权请求已超过有效期（{} 分钟），按失效处理: state={}", ttlMillis / 60000, state);
            removeAttribute(session, state);
            return null;
        }
        return authorizationRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            // Spring Security 传 null 表示清除（如授权被拒绝 / 校验失败）
            HttpSession session = request.getSession(false);
            String state = request.getParameter(OAuth2ParameterNames.STATE);
            if (session != null && state != null) {
                removeAttribute(session, state);
            }
            return;
        }
        String state = authorizationRequest.getState();
        if (state == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        purgeExpired(session);
        session.setAttribute(ATTR_PREFIX + state, authorizationRequest);
        session.setAttribute(ATTR_PREFIX + state + EXPIRES_SUFFIX, System.currentTimeMillis() + ttlMillis);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        HttpSession session = request.getSession(false);
        if (session != null && state != null) {
            removeAttribute(session, state);
        }
        return authorizationRequest;
    }

    private void removeAttribute(HttpSession session, String state) {
        session.removeAttribute(ATTR_PREFIX + state);
        session.removeAttribute(ATTR_PREFIX + state + EXPIRES_SUFFIX);
    }

    /** 清理 Session 中已过期的授权请求条目（顺手做，避免僵尸 state 堆积） */
    private void purgeExpired(HttpSession session) {
        long now = System.currentTimeMillis();
        List<String> stateKeys = new ArrayList<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(ATTR_PREFIX) && !name.endsWith(EXPIRES_SUFFIX)) {
                stateKeys.add(name);
            }
        }
        for (String key : stateKeys) {
            Object expiresAt = session.getAttribute(key + EXPIRES_SUFFIX);
            if (expiresAt instanceof Long expires && now > expires) {
                session.removeAttribute(key);
                session.removeAttribute(key + EXPIRES_SUFFIX);
            }
        }
    }
}