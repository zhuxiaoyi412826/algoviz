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
        // isAnnotation(true)：启用 Sa-Token 方法级注解（@SaCheckLogin/@SaCheckPermission）
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 只匹配需要认证的后台接口
            SaRouter.match(
                            "/api/admin/**",                   // 后台管理全量接口（题解/评论审核等）
                            "/api/algorithm-vector/**",        // 算法题目向量管理（后台，Sa-Token 登录校验）
                            "/api/system/admin/**",
                            "/api/system/profile",
                            "/api/system/system-config",
                            "/api/system/login-log",
                            "/api/system/login-log/**",
                            "/api/system/operation-log",
                            "/api/system/operation-log/**",
                            "/api/export/**",
                            "/api/dashboard/**",
                            "/api/file/**",                   // 文件管理（后台）
                            "/api/users/**",                  // 用户管理（后台）
                            // ===== 补充：此前被 WebConfig 排除 AuthInterceptor 但 Sa-Token 未覆盖的后台接口 =====
                            "/api/content/**",                // 内容管理（数据结构/算法/题库/测试用例/AI提示）
                            "/api/monitor/**",                // 服务监控
                            "/api/payment/records**",         // 支付记录管理（用户支付接口 /api/payment/* 保持公开）
                            "/api/payment/refunds**",
                            "/api/payment/stats**",
                            "/api/payment/trend**",
                            "/api/extension/**",              // 扩展管理（公告/反馈）
                            "/api/interview/admin/**",        // 面试题管理
                            "/api/coin/admin/**",             // 金币商品管理
                            "/api/orders/**",                 // 订单管理
                            "/api/audit/**",                  // 内容审核
                            "/api/vector/admin/**"            // 向量管理（管理端）
                    )
                    .check(r -> StpUtil.checkLogin());

        }).isAnnotation(true)).addPathPatterns("/**")
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
