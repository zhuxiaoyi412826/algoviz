package com.algoviz.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 兼容旧版 Swagger UI 的配置端点
 * 解决前端请求 /v3/api-docs/swagger-config 404 的问题
 */
@RestController
@RequestMapping("/v3/api-docs")
public class SwaggerConfigController {

    @GetMapping("/swagger-config")
    public Map<String, Object> getSwaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("urls", new String[]{"/api-docs"});
        config.put("url", "/api-docs");
        return config;
    }
}