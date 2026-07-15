package com.algoviz.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API 路径保护过滤器 - 确保 API 请求不会被静态资源处理器拦截
 */
@Component
@Order(1)
public class ApiPathFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        
        // 如果是 API 路径，确保不被静态资源处理
        if (path.startsWith("/api/") || path.startsWith("/v3/api-docs/") || path.equals("/api-docs")) {
            // 设置正确的 Content-Type
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setContentType("application/json;charset=UTF-8");
        }
        
        chain.doFilter(request, response);
    }
}