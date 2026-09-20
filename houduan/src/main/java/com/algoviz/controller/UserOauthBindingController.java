package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.entity.User;
import com.algoviz.entity.UserOauth;
import com.algoviz.service.OauthLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心 —— 第三方账号绑定管理（需登录态，路径 /api/account/oauth/** 走 AuthInterceptor）
 *
 * <p>绑定流程：</p>
 * <ol>
 *   <li>前端调 POST /api/account/oauth/bind/{provider} 发起绑定：后端在 Session 写入绑定意图，
 *       并返回 Spring Security OAuth2 的授权跳转地址；</li>
 *   <li>前端 window.location 跳转到该地址，用户在 GitHub/Gitee 完成授权；</li>
 *   <li>回调 /login/oauth2/code/{provider} 由 OAuth2LoginSuccessHandler 处理，检测到 Session
 *       绑定意图后，将第三方账号绑定到当前登录用户（bind_scene=2），302 回个人中心并带结果参数；</li>
 *   <li>若未检测到绑定意图，则走原有「登录/自动注册」逻辑。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/account/oauth")
@Tag(name = "第三方账号绑定", description = "个人中心绑定/解绑 GitHub/Gitee 第三方账号（需登录）")
public class UserOauthBindingController {

    /** Session 中存放「绑定意图」的 key（值 = provider，存在即表示本次 OAuth 回调是绑定而非登录） */
    public static final String SESSION_BIND_INTENT = "oauth_bind_intent";

    @Autowired
    private OauthLoginService oauthLoginService;

    /**
     * 查询当前登录用户已绑定的第三方账号列表
     */
    @GetMapping("/bindings")
    @Operation(summary = "查询已绑定的第三方账号", description = "返回当前用户的 GitHub/Gitee 绑定列表（含昵称、头像、绑定时间、登录次数）")
    public Map<String, Object> listBindings(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = currentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<UserOauth> bindings = oauthLoginService.listBindings(userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserOauth b : bindings) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("provider", b.getProvider());
            item.put("nickname", b.getNickname());
            item.put("avatarUrl", b.getAvatarUrl());
            item.put("loginCount", b.getLoginCount() != null ? b.getLoginCount() : 0);
            item.put("bindScene", b.getBindScene());
            item.put("createdAt", b.getCreatedAt());
            list.add(item);
        }
        result.put("success", true);
        result.put("bindings", list);
        return result;
    }

    /**
     * 发起第三方账号绑定：在 Session 写入绑定意图，返回 OAuth 授权跳转地址。
     * 前端拿到地址后 window.location 跳转。
     */
    @PostMapping("/bind/{provider}")
    @Operation(summary = "发起绑定第三方账号", description = "provider=github|gitee；返回授权跳转地址 authUrl，前端跳转即可")
    public Map<String, Object> startBind(@PathVariable String provider, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = currentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        if (provider == null || (!provider.equalsIgnoreCase("github") && !provider.equalsIgnoreCase("gitee"))) {
            result.put("success", false);
            result.put("message", "不支持的第三方平台");
            return result;
        }
        provider = provider.toLowerCase();
        final String providerKey = provider;

        // 重复绑定预校验
        UserOauth exist = oauthLoginService.listBindings(userId).stream()
                .filter(b -> providerKey.equals(b.getProvider())).findFirst().orElse(null);
        if (exist != null) {
            result.put("success", false);
            result.put("message", "当前账号已绑定该平台，请先解绑再重新绑定");
            return result;
        }

        // 写入 Session 绑定意图（回调时据此走绑定分支）
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_BIND_INTENT, provider);

        // Spring Security OAuth2 的授权端点（无需拼接 redirect_uri，框架自动处理）
        String authUrl = request.getContextPath() + "/oauth2/authorization/" + provider;
        result.put("success", true);
        result.put("authUrl", authUrl);
        return result;
    }

    /**
     * 解绑第三方账号（逻辑删除）
     */
    @DeleteMapping("/bind/{provider}")
    @Operation(summary = "解绑第三方账号", description = "解绑当前用户在指定平台上的绑定")
    public Map<String, Object> unbind(@PathVariable String provider, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = currentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        if (provider == null || (!provider.equalsIgnoreCase("github") && !provider.equalsIgnoreCase("gitee"))) {
            result.put("success", false);
            result.put("message", "不支持的第三方平台");
            return result;
        }
        boolean ok = oauthLoginService.unbind(userId, provider.toLowerCase());
        if (ok) {
            result.put("success", true);
            result.put("message", "已解绑");
        } else {
            result.put("success", false);
            result.put("message", "未找到可解绑的绑定");
        }
        return result;
    }

    /** 从 Session/Cookie 取当前登录用户 id（与 AuthInterceptor 同口径） */
    private Integer currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (attr instanceof User u) {
                return u.getId();
            }
        }
        String uid = AuthInterceptor.getCookieValue(request, AuthInterceptor.COOKIE_USER_ID);
        if (uid != null) {
            try {
                return Integer.parseInt(uid);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
