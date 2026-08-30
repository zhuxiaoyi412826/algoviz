package com.algoviz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private NonceReplayInterceptor nonceReplayInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j 资源映射
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // 管理后台路径
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("file:../houtai/dist/")
                .setCachePeriod(0);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 0) 接口级 IP 限流（防 CC 攻击，最先执行）
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");

        // 0.5) 防重放（nonce + 时间戳 + Redis 去重；未携带 nonce 的请求兼容放行）
        registry.addInterceptor(nonceReplayInterceptor).addPathPatterns("/api/**");

        // 1) 注册登录拦截器：Cookie + Session + 14天双重校验
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 公开接口：登录流程本身、验证码获取、登出等不拦截
                .excludePathPatterns(
                        "/api/captcha",
                        "/api/captcha/**",
                        "/api/login",
                        "/api/login/account",
                        "/api/login/email",
                        "/api/login/email-code",
                        "/api/login/send-email-code",
                        "/api/login/register",
                        "/api/login/verification-code",
                        "/api/login/check-status",
                        "/api/login/logout",
                        // 密码重置（忘记密码、重置密码，不需要登录）
                        "/api/password",
                        "/api/password/**",
                        // 测试接口
                        "/api/password/test-generate-token",
                        // 硬币商品浏览（公开，购买需登录）
                        "/api/coin/products",
                        "/api/coin/products/**",
                        // 后台管理接口（使用独立 Bearer Token 鉴权，不走前台 Cookie/Session）
                        // 注意：Spring Boot 3.x PathPattern 中 /xxx/** 不匹配 /xxx 本身，需同时排除
                        "/api/admin",
                        "/api/admin/**",
                        "/api/system",
                        "/api/system/**",
                        "/api/content",
                        "/api/content/**",
                        "/api/payment",
                        "/api/payment/**",
                        "/api/orders",
                        "/api/orders/**",
                        "/api/coin/admin",
                        "/api/coin/admin/**",
                        "/api/interview",
                        "/api/interview/**",
                        "/api/users",
                        "/api/users/**",
                        // OJ 题目管理（后台管理接口，使用独立 Bearer Token 鉴权）
                        "/api/problems",
                        "/api/problems/**",
                        // OJ 用户题解（前端用 X-User-Id header 传身份，不走 Cookie/Session）
                        "/api/solutions",
                        "/api/solutions/**",
                        // 向量检索服务（管理端 Bearer Token 鉴权，用户端公开搜索）
                        "/api/vector",
                        "/api/vector/**",
                        // 算法题目向量管理（后台，走 Sa-Token 鉴权）
                        "/api/algorithm-vector",
                        "/api/algorithm-vector/**",
                        // 内容审核系统（后台 Bearer Token 鉴权）
                        "/api/audit",
                        "/api/audit/**",
                        // 反馈管理（后台接口）
                        "/api/feedback",
                        "/api/feedback/**",
                        // 公告管理（后台接口）
                        "/api/announcements",
                        "/api/announcements/**",
                        // 更新日志（后台 CRUD，Sa-Token 做管理员鉴权；公开列表另走 /api/public/changelog）
                        "/api/changelog",
                        "/api/changelog/**",
                        // 仪表盘数据（后台接口）
                        "/api/dashboard",
                        "/api/dashboard/**",
                        // 前台公开接口（页脚动态配置等）
                        "/api/public",
                        "/api/public/**",
                        // Knife4j / Swagger 文档资源
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/favicon.ico"
                );
    }
}
