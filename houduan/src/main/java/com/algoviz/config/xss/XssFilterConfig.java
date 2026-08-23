package com.algoviz.config.xss;

import com.algoviz.config.XssProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * XSS 全局过滤器配置：
 *   - 仅当 xss.enabled=true（默认）时生效
 *   - 命中 xss.exclude-urls 列表的请求跳过包装
 *   - Filter 顺序：放在 TraceIdFilter 之后、ApiPathFilter 之前（Order -9999）
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(XssProperties.class)
public class XssFilterConfig {

    @Bean
    @ConditionalOnProperty(prefix = "xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilterRegistration(XssProperties xssProperties) {
        FilterRegistrationBean<XssFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new XssFilter(xssProperties));
        reg.addUrlPatterns("/*");
        reg.setName("xssFilter");
        reg.setOrder(-9999);
        log.info("[XSS] 全局过滤器已启用，excludeUrls={}", xssProperties.getExcludeUrls());
        return reg;
    }

    @RequiredArgsConstructor
    public static class XssFilter extends OncePerRequestFilter {

        private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

        private final XssProperties properties;

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                uri = uri.substring(contextPath.length());
            }
            // 命中排除列表：直接放行
            if (isExcluded(uri)) {
                chain.doFilter(request, response);
                return;
            }
            chain.doFilter(new XssHttpServletRequestWrapper(request), response);
        }

        private boolean isExcluded(String uri) {
            for (String pattern : properties.getExcludeUrls()) {
                if (PATH_MATCHER.match(pattern, uri)) {
                    return true;
                }
            }
            return false;
        }
    }
}
