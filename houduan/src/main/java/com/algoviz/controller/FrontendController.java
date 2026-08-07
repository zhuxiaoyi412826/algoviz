package com.algoviz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端页面控制器 - 处理根路径访问
 */
@RestController
@Tag(name = "前端页面", description = "前端页面入口与静态资源访问")
public class FrontendController {

    @GetMapping("/")
    @Operation(summary = "首页入口", description = "访问根路径时返回前端 index.html 页面")
    public ResponseEntity<Resource> index() {
        // 尝试加载前端 index.html
        Resource resource = new ClassPathResource("static/index.html");
        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        }
        
        // 返回简单的欢迎页面
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("public/index.html"));
    }
}