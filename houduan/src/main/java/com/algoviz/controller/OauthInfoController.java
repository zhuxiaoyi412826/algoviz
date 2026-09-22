package com.algoviz.controller;

import com.algoviz.config.security.ConfigurableClientRegistrationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方授权登录辅助接口（公开）
 *
 * <p>返回当前后端已注册并配置了 clientId/secret 的 OAuth 平台列表；
 * 一个平台都未配置时返回空数组，前端据此禁用对应的第三方登录按钮并提示不可用。</p>
 */
@RestController
@RequestMapping("/api/oauth")
@Tag(name = "第三方授权登录", description = "OAuth 平台可用性查询（登录入口请直接跳转 /oauth2/authorization/{平台}）")
public class OauthInfoController {

    @Autowired
    private ConfigurableClientRegistrationRepository clientRegistrationRepository;

    /** OAuth 授权/回调基地址（application.yml: app.oauth-redirect-base），前端登录入口按此拼授权地址 */
    @Value("${app.oauth-redirect-base:}")
    private String oauthRedirectBase;

    @GetMapping("/providers")
    @Operation(summary = "已配置的 OAuth 平台列表", description = "返回 [github, gitee] 中已配置 client 的平台；未配置则返回空数组")
    public Map<String, Object> providers() {
        List<String> providers = new ArrayList<>();
        for (ClientRegistration registration : clientRegistrationRepository) {
            providers.add(registration.getRegistrationId());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("providers", providers);
        // 授权入口基地址：必须与回调地址同源，否则授权请求 Session 与回调 Session 不在同一个 host，
        // 会以 authorization_request_not_found 失败（前端若自行按 window.location.hostname 拼，
        // 从 127.0.0.1 访问时会得到 127.0.0.1:80，与登记的 localhost:80 回调不匹配）
        result.put("authorizeBaseUrl", oauthRedirectBase);
        return result;
    }
}
