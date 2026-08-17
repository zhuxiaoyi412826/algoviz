package com.algoviz.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 全链路追踪过滤器
 *
 * <p>为每个 HTTP 请求生成或提取 TraceId，放入 SLF4J MDC，
 * 使日志框架自动在每一行日志中输出 trace_id 字段。
 * 同时将 TraceId 写入响应头，方便前端或下游服务获取。</p>
 *
 * <p>优先级设为最高（@Order(Integer.MIN_VALUE)），确保在所有其他过滤器之前执行。</p>
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceIdFilter implements Filter {

    /** TraceId 在 HTTP 请求头中的名称（与 Python 端约定一致） */
    public static final String TRACE_HEADER = "X-Trace-Id";

    /** TraceId 在 SLF4J MDC 中的键名（logback 中用 %X{trace_id} 引用） */
    public static final String MDC_KEY = "trace_id";

    /** TraceId 响应头名称，方便前端调试 */
    public static final String TRACE_RESPONSE_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. 尝试从请求头获取 TraceId（跨服务传递场景）
        String traceId = httpRequest.getHeader(TRACE_HEADER);

        // 2. 如果没有，从 MDC 中获取（极少数情况）
        if (traceId == null || traceId.isEmpty()) {
            traceId = MDC.get(MDC_KEY);
        }

        // 3. 仍然没有，生成新的 TraceId
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // 4. 放入 MDC，后续所有日志自动携带
        MDC.put(MDC_KEY, traceId);

        // 5. 写入响应头，方便前端获取
        if (response instanceof jakarta.servlet.http.HttpServletResponse) {
            ((jakarta.servlet.http.HttpServletResponse) response)
                    .setHeader(TRACE_RESPONSE_HEADER, traceId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 6. 请求结束后清理 MDC，防止线程池复用时 TraceId 串号
            MDC.remove(MDC_KEY);
        }
    }
}
