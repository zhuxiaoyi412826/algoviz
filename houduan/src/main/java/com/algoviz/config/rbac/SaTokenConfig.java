package com.algoviz.config.rbac;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截配置
 *
 * 认证分级：
 *  1. 公开路径（白名单）：完全放行（登录、公共API、Swagger、静态资源）
 *  2. 后台管理员接口：Sa-Token StpUtil.checkLogin() + 注解权限
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 只匹配需要认证的后台接口
            SaRouter.match(
                            "/api/admin/**",                   // 后台管理全量接口（题解/评论审核等）
                            "/api/system/admin/**",
                            "/api/system/profile",
                            "/api/system/system-config",
                            "/api/system/login-log",
                            "/api/system/login-log/**",
                            "/api/system/operation-log",
                            "/api/system/operation-log/**",
                            "/api/export/**",
                            "/api/dashboard/**"
                    )
                    .check(r -> StpUtil.checkLogin());

        })).addPathPatterns("/**")
                // 白名单：完全放行
                .excludePathPatterns(
                        "/api/admin/login",
                        "/api/admin/register",
                        "/api/admin/info",    // 获取当前登录用户信息（Sa-Token 会自动处理）
                        "/api/admin/logout",  // 登出接口
                        "/api/login/**",
                        "/api/public/**",
                        "/api/solutions/**",
                        "/api/submissions/**",
                        "/api/statistics/**",
                        "/api/announcements/**",
                        "/api/feedback/**",
                        "/api/interview/user/**",
                        "/api/register/**",
                        "/api/payment/**",
                        // Knife4j / Swagger / SpringDoc
                        "/doc.html",
                        "/doc.html/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/favicon.ico",
                        "/error",
                        // 静态资源
                        "/static/**",
                        "/images/**",
                        "/js/**",
                        "/css/**",
                        "/"
                );
    }
}
