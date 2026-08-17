package com.algoviz.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Feign 客户端 TraceId 传递拦截器
 *
 * <p>当 Spring Boot 通过 OpenFeign 调用 Python 向量服务时，
 * 自动从 SLF4J MDC 中获取当前请求的 TraceId，
 * 并作为 HTTP Header {@code X-Trace-Id} 传递给下游服务。</p>
 *
 * <p>这样在 ELK 中搜索同一个 trace_id 就能串起：
 * Java 服务日志 → Feign 调用 → Python 服务日志 的完整链路。</p>
 */
@Component
public class FeignTracingInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceIdFilter.TRACE_HEADER, traceId);
        }
    }
}
