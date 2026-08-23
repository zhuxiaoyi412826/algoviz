package com.algoviz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 全局过滤配置
 * application.yml 示例：
 * xss:
 *   enabled: true
 *   exclude-urls:
 *     - /actuator/**
 *     - /api/admin/upload/**
 */
@Data
@ConfigurationProperties(prefix = "xss")
public class XssProperties {

    /** 是否开启 XSS 过滤（默认开启） */
    private boolean enabled = true;

    /** 不做 XSS 清洗的 URL（AntPathMatcher 风格通配符），例如文件上传接口 / 纯二进制 / 第三方回调接口 */
    private List<String> excludeUrls = new ArrayList<>();
}
