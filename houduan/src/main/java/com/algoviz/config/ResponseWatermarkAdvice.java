package com.algoviz.config;

import cn.dev33.satoken.stp.StpUtil;
import com.algoviz.annotation.ResponseWatermark;
import com.algoviz.entity.rbac.SysUser;
import com.algoviz.mapper.rbac.SysUserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 接口响应水印：{@link ResponseBodyAdvice} 全局响应切面
 *
 * <p><b>职责：</b>仅对标注了 {@link ResponseWatermark} 的 Controller 方法，
 * 在 JSON 响应<b>根节点</b>追加 {@code _watermark} 水印对象，
 * 用于数据泄露事后溯源，也可作为前端页面水印数据源。</p>
 *
 * <p><b>水印内容：</b>userId / username / tenantId / clientIp / traceId / accessTime
 * （userId、username 从 Sa-Token 登录上下文获取）。</p>
 *
 * <p><b>边界处理：</b>
 * ① 返回 String / 文件下载 / void / byte[] / Resource / 流式响应 —— 跳过（非 JSON 消息转换器在
 *    {@link #supports} 中已被排除，此处再防御性兜底）；
 * ② 统一返回体结构适配 —— 把原始响应对象序列化为 JSON 树后，仅在根节点新增 {@code _watermark}，
 *    不修改原 VO/DTO 实体、不把水印嵌入 data 业务对象内部；
 * ③ 未标注注解的接口 —— {@link #supports} 返回 false，原样返回不做任何处理。</p>
 *
 * <p><b>统一返回体示例 JSON（以 ApiResponse 为例）：</b></p>
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ...原有业务数据... },
 *   "_watermark": {
 *     "userId": 1,
 *     "username": "algovize",
 *     "tenantId": "default",
 *     "clientIp": "127.0.0.1",
 *     "traceId": "a1b2c3d4e5f60718",
 *     "accessTime": "2026-09-06 10:00:00.123"
 *   }
 * }
 * }</pre>
 *
 * <p><b>安全备注：</b>接口水印属于<b>事后溯源取证</b>能力，客户端可自行删除/篡改
 * {@code _watermark} 字段，无法阻止泄露；它不能替代权限校验（Sa-Token
 * checkLogin / checkPermission），需搭配安全审计日志（OperationLogAspect）形成闭环。</p>
 */
@RestControllerAdvice
public class ResponseWatermarkAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(ResponseWatermarkAdvice.class);

    /** JSON 根节点水印字段名（约定，前端与后端保持一致） */
    public static final String WATERMARK_FIELD = "_watermark";

    /** 访问时间格式（含毫秒，便于精确溯源比对日志） */
    private static final DateTimeFormatter ACCESS_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 部署环境/租户标识：用于区分同一套代码不同部署环境的泄露溯源。
     * 可通过 application.yml 配置 algoviz.watermark.tenant-id 覆盖（缺省 default）。
     */
    @Value("${algoviz.watermark.tenant-id:default}")
    private String tenantId;

    /* ==================== ① 哪些响应需要进入水印处理 ==================== */

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> selectedConverterType) {
        // 边界①：只处理 JSON 输出（排除 String 专用转换器 / 文件下载 / SSE 等）
        if (!MappingJackson2HttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            return false;
        }
        // 边界③：只有方法上标注 @ResponseWatermark 且 enable=true 才处理，其余原样返回
        Method method = returnType.getMethod();
        if (method == null) {
            return false;
        }
        ResponseWatermark anno = method.getAnnotation(ResponseWatermark.class);
        return anno != null && anno.enable();
    }

    /* ==================== ② 写入水印 ==================== */

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            // 边界①：null(void)/String/二进制/文件/流 直接放行
            if (body == null || body instanceof String
                    || body instanceof byte[] || body instanceof Resource) {
                return body;
            }

            // 边界②：统一返回体适配——序列化为 JSON 树，只改根节点（不触碰 data 内部结构）
            JsonNode tree;
            if (body instanceof ObjectNode node) {
                // 深拷贝：防止直接修改被复用的 ObjectNode（如接口返回缓存节点）
                tree = node.deepCopy();
            } else {
                tree = objectMapper.valueToTree(body);
            }
            // 根节点必须是 JSON 对象（对象数组等无法追加键），原样返回
            if (!tree.isObject()) {
                return body;
            }
            ObjectNode root = (ObjectNode) tree;
            root.remove(WATERMARK_FIELD);          // 防重复写入，先清后加
            root.set(WATERMARK_FIELD, buildWatermark(request));

            return root;
        } catch (Exception e) {
            // 水印失败不阻断业务：原样返回并告警（仅损失取证字段）
            log.warn("[接口水印] 写入失败，响应按原样返回: {}", e.getMessage());
            return body;
        }
    }

    /* ==================== 水印内容组装 ==================== */

    /** 组装根节点水印对象（字段顺序即 JSON 输出顺序） */
    private ObjectNode buildWatermark(ServerHttpRequest serverRequest) {
        ObjectNode wm = objectMapper.createObjectNode();

        // --- 登录用户信息：从 Sa-Token 登录上下文获取（loginId = sys_user.id） ---
        if (StpUtil.isLogin()) {
            try {
                long userId = StpUtil.getLoginIdAsLong();
                wm.put("userId", userId);
                wm.put("username", resolveUsername(userId));
            } catch (Exception e) {
                // 极少情况：token 异常导致读不到登录态，退化为未登录占位
                wm.put("userId", 0);
                wm.put("username", "unknown");
            }
        } else {
            wm.put("userId", 0);
            wm.put("username", "anonymous");
        }

        // --- 环境/租户标识（部署配置） ---
        wm.put("tenantId", tenantId == null || tenantId.isEmpty() ? "default" : tenantId);

        // --- 客户端 IP（与 OperationLogAspect 同一取法，兼容反向代理） ---
        HttpServletRequest servletRequest = currentHttpRequest(serverRequest);
        wm.put("clientIp", resolveClientIp(servletRequest));

        // --- traceId：优先取 MDC（TraceIdFilter 已写入），否则回退请求头 ---
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId == null && servletRequest != null) {
            traceId = servletRequest.getHeader(TraceIdFilter.TRACE_HEADER);
        }
        wm.put("traceId", traceId == null || traceId.isEmpty() ? "-" : traceId);

        // --- 访问时间（毫秒精度） ---
        wm.put("accessTime", LocalDateTime.now().format(ACCESS_TIME_FMT));

        return wm;
    }

    /** 按 sys_user.id 查登录用户名（回退 unknown），与操作日志切面同口径 */
    private String resolveUsername(long userId) {
        try {
            SysUser user = sysUserMapper.findById(userId);
            return user != null && user.getUsername() != null ? user.getUsername() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** 从 ServerHttpRequest 取底层 HttpServletRequest（兜底走 RequestContextHolder） */
    private HttpServletRequest currentHttpRequest(ServerHttpRequest serverRequest) {
        if (serverRequest instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest();
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 解析真实客户端 IP：X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP → remoteAddr */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 代理链路取第一个真实 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null || ip.isEmpty() ? "unknown" : ip;
    }
}
