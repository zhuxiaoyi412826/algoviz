package com.algoviz.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三方授权登录（Spring Security OAuth2 Client）配置 —— 与既有鉴权双轨共存 + 优雅降级
 *
 * <p><b>兼容性设计（关键）</b>：</p>
 * <ul>
 *   <li>本项目鉴权体系为 前台 AuthInterceptor(Session/Cookie) + 后台 Sa-Token，二者均<b>不走</b> Spring Security。</li>
 *   <li>引入 oauth2-client 会连带激活 spring-security，因此本配置让 SecurityFilterChain 仅做两件事：
 *       ① 对 <b>所有请求放行</b>（authorizeHttpRequests permitAll）；
 *       ② 仅在配置了 OAuth 平台时，对授权/回调端点启用 oauth2Login。</li>
 *   <li>业务接口鉴权仍由 AuthInterceptor / Sa-Token 负责，互不干扰。</li>
 * </ul>
 *
 * <p><b>降级保护（在新电脑未配置时主程序照常启动）</b>：</p>
 * <ul>
 *   <li>注册仓库使用 {@link ConfigurableClientRegistrationRepository}（允许为空，始终存在 Bean，
 *       满足 spring-security-config 对仓库 Bean 的强制要求，避免启动失败）；</li>
 *   <li>环境变量未配置 → 仓库为空 → 不挂接 oauth2Login → GitHub/Gitee 登录不可用；</li>
 *   <li>前端通过 GET /api/oauth/providers 探测到空列表后禁用按钮并提示。</li>
 * </ul>
 *
 * <p><b>注册来源</b>：从环境变量读取客户端凭据（禁止明文入库）：</p>
 * <pre>
 *   OAUTH_GITHUB_CLIENT_ID / OAUTH_GITHUB_CLIENT_SECRET   （GitHub 内置端点）
 *   OAUTH_GITEE_CLIENT_ID  / OAUTH_GITEE_CLIENT_SECRET    （Gitee 需手动补齐端点）
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SecurityConfig.class);

    @Value("${OAUTH_GITHUB_CLIENT_ID:}")
    private String githubClientId;
    @Value("${OAUTH_GITHUB_CLIENT_SECRET:}")
    private String githubClientSecret;

    @Value("${OAUTH_GITEE_CLIENT_ID:}")
    private String giteeClientId;
    @Value("${OAUTH_GITEE_CLIENT_SECRET:}")
    private String giteeClientSecret;

    /**
     * OAuth 回调地址前缀（不含 /login/oauth2/code/{registrationId}）。
     * 必须与 GitHub / Gitee 应用后台填写的「回调地址」完全一致（精确匹配，否则平台校验失败），
     * 本地开发：http://localhost:80 ；生产改为正式域名（如 https://dsaol.asia）。
     * 固定写死而非用 {baseUrl} 模板：避免 localhost 与 localhost:80 因端口省略产生两套不一致的回调。
     */
    @Value("${app.oauth-redirect-base:http://localhost:80}")
    private String oauthRedirectBase;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Autowired
    private OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    /**
     * 仓库始终存在（允许为空）；过滤链据此决定是否挂接 oauth2Login。
     */
    @Autowired
    private ObjectProvider<ConfigurableClientRegistrationRepository> clientRegistrationRepositoryProvider;

    /**
     * OAuth2 Client 注册表：GitHub(内置) + Gitee(手动端点)
     * 只有 clientId/secret 均已配置的平台才会被注册；全部未配置时仓库为空但不影响启动。
     */
    @Bean
    public ConfigurableClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        // GitHub：Spring 内置 CommonOAuth2Provider，authorize/token/userinfo 端点自动补齐
        if (StringUtils.hasText(githubClientId) && StringUtils.hasText(githubClientSecret)) {
            ClientRegistration github = CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(githubClientId)
                    .clientSecret(githubClientSecret)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("read:user", "user:email")
                    .build();
            registrations.add(github);
            log.info("OAuth2 已注册平台: github (回调 /login/oauth2/code/github)");
        } else {
            log.warn("未配置 OAUTH_GITHUB_CLIENT_ID/SECRET，GitHub 登录不可用");
        }

        // Gitee：非内置平台，手动补齐三个端点 + user-name-attribute
        if (StringUtils.hasText(giteeClientId) && StringUtils.hasText(giteeClientSecret)) {
            ClientRegistration gitee = ClientRegistration.withRegistrationId("gitee")
                    .clientId(giteeClientId)
                    .clientSecret(giteeClientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(oauthRedirectBase + "/login/oauth2/code/{registrationId}")
                    .scope("user_info")
                    .authorizationUri("https://gitee.com/oauth/authorize")
                    .tokenUri("https://gitee.com/oauth/token")
                    .userInfoUri("https://gitee.com/api/v5/user")
                    .userNameAttributeName("id")
                    .clientName("Gitee")
                    .build();
            registrations.add(gitee);
            log.info("OAuth2 已注册平台: gitee (回调 /login/oauth2/code/gitee)");
        } else {
            log.warn("未配置 OAUTH_GITEE_CLIENT_ID/SECRET，Gitee 登录不可用");
        }

        // 全部平台都未配置：仍返回空仓库 Bean，主程序照常启动（第三方登录降级不可用）
        if (registrations.isEmpty()) {
            log.warn("未配置任何 OAuth2 平台（OAUTH_GITHUB_* / OAUTH_GITEE_*），第三方登录不可用，主程序正常启动");
        }
        return new ConfigurableClientRegistrationRepository(registrations);
    }

    /**
     * Spring Security 过滤链（兼容层）：
     * 全部放行；仅在配置了至少一个 OAuth 平台时挂接 oauth2Login（授权端点 /oauth2/authorization/{id}
     * + 回调 /login/oauth2/code/{id}），未配置则完全不暴露 OAuth 端点。
     */
    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 本项目接口鉴权走既有机制，关闭 Spring Security 的 csrf/formLogin/httpBasic/logout
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // 关键：对业务接口一律放行，避免影响前台 Session / 后台 Sa-Token
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        ConfigurableClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (repository != null && !repository.isEmpty()) {
            http.oauth2Login(oauth -> oauth
                    .clientRegistrationRepository(repository)
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler(oauth2LoginFailureHandler));
        }
        return http.build();
    }
}
