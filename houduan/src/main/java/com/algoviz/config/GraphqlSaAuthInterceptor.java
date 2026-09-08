package com.algoviz.config;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphQL 接口登录校验（/graphql, /graphql/**）：
 * GraphQL 与 REST 同走 DispatcherServlet，但数据加载器由 spring-graphql 内部调用，
 * 直接在 @Controller 上打 @SaCheckLogin 不生效，因此用 MVC 拦截器在进入处理器前
 * 统一校验 Sa-Token（header: satoken）。GraphiQL 面板页面本身不拦截，
 * 其发出的 /graphql 请求需在面板中配置 satoken header。
 */
@Component
public class GraphqlSaAuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        boolean isGraphql = "/graphql".equals(uri) || uri.startsWith("/graphql/");
        if (!isGraphql) {
            return true;
        }
        try {
            StpUtil.checkLogin();
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("code", 401);
            body.put("message", "未登录或登录已过期，请重新登录");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
            return false;
        }
    }
}
