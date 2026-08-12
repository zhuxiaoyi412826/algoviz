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
        // 注册登录拦截器：Cookie + Session + 14天双重校验
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 公开接口：登录流程本身、验证码获取、登出等不拦截
                .excludePathPatterns(
                        "/api/login",
                        "/api/login/account",
                        "/api/login/verification-code",
                        "/api/login/check-status",
                        "/api/login/logout",
                        // Knife4j / Swagger 文档资源
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/favicon.ico"
                );
    }
}