package com.algoviz.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 按 registrationId 维度开关 PKCE 的授权请求解析器
 *
 * <p>Spring Security 6.2 只提供 {@code OAuth2AuthorizationRequestCustomizers.withPkce()} 这种
 * 「整体开关」（挂在 resolver 上对所有平台生效），而本项目 GitHub 支持 PKCE、Gitee 的 token
 * 端点不接受 code_verifier（未开放 PKCE），必须逐平台区分。</p>
 *
 * <p>做法：委托给标准的 {@code DefaultOAuth2AuthorizationRequestResolver}（它是 final，
 * 只能组合不能继承）完成解析，再对启用了 PKCE 的平台重建 Builder 并套用官方 PKCE 定制器
 * （追加 code_challenge / code_challenge_method=S256，并把 code_verifier 存进授权请求属性，
 * 换取 token 时由 Spring 自动带上）。</p>
 *
 * <p>注意：{@code OAuth2AuthorizationRequest.from(...)} 不会复制 authorizationRequestUri，
 * 因此 build() 会基于 additionalParameters 重新拼装跳转 URL，PKCE 参数能正确出现在授权链接上。</p>
 */
public class PkceAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    /** 标准解析器（final 类，通过组合复用） */
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    /** 官方 PKCE 定制器（S256；内部有幂等判断，重复套用不会叠加参数） */
    private final Consumer<OAuth2AuthorizationRequest.Builder> pkceCustomizer =
            OAuth2AuthorizationRequestCustomizers.withPkce();

    /** 启用 PKCE 的平台 registrationId 集合 */
    private final Set<String> pkceRegistrations;

    public PkceAwareAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                Set<String> pkceRegistrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        this.pkceRegistrations = pkceRegistrations == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(pkceRegistrations);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        // 只解析一次：delegate 每次都会生成新的 state
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        return applyPkce(authorizationRequest, registrationIdOf(authorizationRequest));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return applyPkce(delegate.resolve(request, registrationId), registrationId);
    }

    private OAuth2AuthorizationRequest applyPkce(OAuth2AuthorizationRequest authorizationRequest, String registrationId) {
        if (authorizationRequest == null || registrationId == null
                || !pkceRegistrations.contains(registrationId)) {
            return authorizationRequest;
        }
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);
        pkceCustomizer.accept(builder);
        return builder.build();
    }

    /** 从已解析出的授权请求属性中取 registrationId（标准解析器会写入 registration_id） */
    private static String registrationIdOf(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }
        Object value = authorizationRequest.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
        return value == null ? null : value.toString();
    }
}