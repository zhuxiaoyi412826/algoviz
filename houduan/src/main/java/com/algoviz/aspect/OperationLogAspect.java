package com.algoviz.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.algoviz.entity.rbac.SysUser;
import com.algoviz.mapper.OperationLogMapper;
import com.algoviz.mapper.rbac.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 操作日志 AOP 切面
 * 自动记录所有管理员写操作（POST/PUT/DELETE）到 operation_log 表
 * 排除：登录、退出、验证码、操作日志查询等接口
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 拦截所有 Controller 方法
     */
    @Pointcut("execution(* com.algoviz.controller..*.*(..))")
    public void controllerAll() {}

    /**
     * 模块映射表：URL 路径前缀 → 模块名
     */
    private static final Map<String, String> MODULE_MAP = new HashMap<>();
    static {
        MODULE_MAP.put("/api/system/admin", "用户管理");
        MODULE_MAP.put("/api/system/roles", "用户管理");
        MODULE_MAP.put("/api/system/config", "系统配置");
        MODULE_MAP.put("/api/system/login-log", "系统监控");
        MODULE_MAP.put("/api/system/operation-log", "系统监控");
        MODULE_MAP.put("/api/system/product", "商品管理");
        MODULE_MAP.put("/api/system/coin", "金币管理");
        MODULE_MAP.put("/api/system/announcement", "公告管理");
        MODULE_MAP.put("/api/system/feedback", "反馈管理");
        MODULE_MAP.put("/api/system/audit", "内容审核");
        MODULE_MAP.put("/api/audit", "内容审核");
        MODULE_MAP.put("/api/admin/content", "内容审核");
        MODULE_MAP.put("/api/product", "商品管理");
        MODULE_MAP.put("/api/coin", "金币管理");
        MODULE_MAP.put("/api/announcement", "公告管理");
        MODULE_MAP.put("/api/feedback", "反馈管理");
        MODULE_MAP.put("/api/order", "订单管理");
        MODULE_MAP.put("/api/payment", "支付管理");
        MODULE_MAP.put("/api/user", "用户管理");
        MODULE_MAP.put("/api/interview", "面试题管理");
        MODULE_MAP.put("/api/oj", "OJ题目管理");
        MODULE_MAP.put("/api/dashboard", "仪表盘");
        MODULE_MAP.put("/api/ai", "AI对话");
        MODULE_MAP.put("/api/code", "代码运行");
        MODULE_MAP.put("/api/vector", "向量检索");
    }

    /**
     * 排除的路径（不记录操作日志）
     */
    private static final String[] EXCLUDE_PATHS = {
            "/api/admin/login",
            "/api/admin/logout",
            "/api/admin/info",
            "/api/system/login-log",
            "/api/system/operation-log",
            "/api/captcha",
            "/api/login",
            "/api/register",
            "/api/password/",
            "/api/dashboard",
            "/api/announcements",
            "/api/feedback",
            "/api/product",
            "/api/coin/",
            "/api/order/",
    };

    /**
     * 排除的 HTTP 方法
     */
    private static final String[] EXCLUDE_HTTP_METHODS = {"GET", "HEAD", "OPTIONS"};

    @Around("controllerAll()")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        // 检查是否为排除的 HTTP 方法
        String httpMethod = request.getMethod().toUpperCase();
        for (String excluded : EXCLUDE_HTTP_METHODS) {
            if (excluded.equalsIgnoreCase(httpMethod)) {
                return joinPoint.proceed();
            }
        }

        String requestURI = request.getRequestURI();

        // 检查是否为排除的路径
        for (String excludePath : EXCLUDE_PATHS) {
            if (requestURI.startsWith(excludePath)) {
                return joinPoint.proceed();
            }
        }

        // 登录/退出相关路径排除
        if (requestURI.contains("/login") || requestURI.contains("/logout")
                || requestURI.contains("/captcha") || requestURI.contains("/send-email-code")) {
            return joinPoint.proceed();
        }

        // 操作日志查询接口排除
        if (requestURI.contains("operation-log") && "GET".equalsIgnoreCase(httpMethod)) {
            return joinPoint.proceed();
        }

        // 未登录用户不记录（可能是公共写接口）
        if (!StpUtil.isLogin()) {
            return joinPoint.proceed();
        }

        try {
            return joinPoint.proceed();
        } finally {
            try {
                saveOperationLog(request, httpMethod, requestURI, method, joinPoint);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(HttpServletRequest request, String httpMethod,
                                   String requestURI, Method method,
                                   ProceedingJoinPoint joinPoint) {
        // 获取 @LogOperation 注解
        LogOperation logAnno = method.getAnnotation(LogOperation.class);

        com.algoviz.entity.OperationLog opLog = new com.algoviz.entity.OperationLog();
        opLog.setId(UUID.randomUUID().toString().replace("-", ""));

        // 设置操作人信息
        try {
            long userId = StpUtil.getLoginIdAsLong();
            opLog.setUserId(String.valueOf(userId));
            SysUser user = sysUserMapper.findById(userId);
            if (user != null) {
                opLog.setUsername(user.getUsername());
            } else {
                opLog.setUsername("unknown");
            }
        } catch (Exception e) {
            opLog.setUserId("0");
            opLog.setUsername("unknown");
        }

        // 设置模块
        String module;
        if (logAnno != null && !logAnno.module().isEmpty()) {
            module = logAnno.module();
        } else {
            module = resolveModule(requestURI);
        }
        opLog.setModule(module);

        // 设置操作类型
        String action;
        if (logAnno != null && !logAnno.action().isEmpty()) {
            action = logAnno.action();
        } else {
            action = resolveAction(httpMethod, requestURI);
        }
        opLog.setAction(action);

        // 设置详情
        String detail;
        if (logAnno != null && !logAnno.detail().isEmpty()) {
            detail = logAnno.detail();
        } else {
            detail = resolveDetail(httpMethod, requestURI, method, joinPoint);
        }
        opLog.setDetail(detail);

        // 设置 IP
        opLog.setIp(getClientIp(request));

        // 设置时间
        opLog.setCreatedAt(LocalDateTime.now().toString().replace("T", " ").substring(0, 19));

        operationLogMapper.insert(opLog);
    }

    /**
     * 根据 URL 路径推断模块名
     */
    private String resolveModule(String requestURI) {
        for (Map.Entry<String, String> entry : MODULE_MAP.entrySet()) {
            if (requestURI.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 从 URL 提取模块：/api/{module}/...
        String[] parts = requestURI.split("/");
        if (parts.length >= 3) {
            String candidate = parts[2];
            switch (candidate) {
                case "system":
                    return "系统管理";
                case "admin":
                    return "系统管理";
                case "user":
                    return "用户管理";
                case "interview":
                    return "面试题管理";
                case "oj":
                    return "OJ题目管理";
                case "audit":
                    return "内容审核";
                case "announcement":
                    return "公告管理";
                case "feedback":
                    return "反馈管理";
                case "order":
                    return "订单管理";
                case "payment":
                    return "支付管理";
                case "product":
                    return "商品管理";
                case "coin":
                    return "金币管理";
                default:
                    return candidate;
            }
        }
        return "其他";
    }

    /**
     * 根据 HTTP 方法推断操作类型
     */
    private String resolveAction(String httpMethod, String requestURI) {
        if ("POST".equalsIgnoreCase(httpMethod)) {
            return "新增";
        } else if ("PUT".equalsIgnoreCase(httpMethod)) {
            if (requestURI.contains("status") || requestURI.contains("enable") || requestURI.contains("disable")) {
                return requestURI.contains("disable") ? "禁用" : "启用";
            }
            return "编辑";
        } else if ("DELETE".equalsIgnoreCase(httpMethod)) {
            return "删除";
        } else if ("PATCH".equalsIgnoreCase(httpMethod)) {
            return "编辑";
        }
        return "操作";
    }

    /**
     * 根据 URL 路径和方法生成操作详情
     */
    private String resolveDetail(String httpMethod, String requestURI,
                                 Method method, ProceedingJoinPoint joinPoint) {
        StringBuilder detail = new StringBuilder();
        String methodName = method.getName();

        // 尝试从 URL 提取资源标识
        String resourceId = extractResourceId(requestURI);
        String resourceName = extractResourceName(requestURI);

        String action = resolveAction(httpMethod, requestURI);

        // 常见操作的中文描述
        switch (action) {
            case "新增":
                if (resourceName != null) {
                    detail.append("创建").append(resourceName);
                } else {
                    detail.append("执行新增操作");
                }
                break;
            case "编辑":
                if (resourceName != null && resourceId != null) {
                    detail.append("修改").append(resourceName).append("：").append(resourceId);
                } else if (resourceName != null) {
                    detail.append("修改").append(resourceName);
                } else {
                    detail.append("执行编辑操作");
                }
                break;
            case "删除":
                if (resourceName != null && resourceId != null) {
                    detail.append("删除").append(resourceName).append("：").append(resourceId);
                } else if (resourceId != null) {
                    detail.append("删除记录：").append(resourceId);
                } else {
                    detail.append("执行删除操作");
                }
                break;
            case "启用":
                if (resourceName != null && resourceId != null) {
                    detail.append("启用").append(resourceName).append("：").append(resourceId);
                } else {
                    detail.append("执行启用操作");
                }
                break;
            case "禁用":
                if (resourceName != null && resourceId != null) {
                    detail.append("禁用").append(resourceName).append("：").append(resourceId);
                } else {
                    detail.append("执行禁用操作");
                }
                break;
            default:
                detail.append("执行").append(action).append("操作");
        }

        // 尝试从请求体提取更多信息
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) arg;
                        // 提取用户名、标题等关键字段
                        String[] keyFields = {"username", "realName", "name", "title", "problemNo", "email"};
                        for (String key : keyFields) {
                            Object val = map.get(key);
                            if (val != null) {
                                detail.append("，").append(key).append("：").append(val);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return detail.toString();
    }

    /**
     * 从 URL 中提取资源 ID（最后一个路径段）
     */
    private String extractResourceId(String requestURI) {
        String[] parts = requestURI.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (!part.isEmpty() && part.matches("\\d+")) {
                return part;
            }
        }
        return null;
    }

    /**
     * 从 URL 中推断资源名称
     */
    private String extractResourceName(String requestURI) {
        if (requestURI.contains("admin")) {
            return "管理员";
        } else if (requestURI.contains("user") || requestURI.contains("users")) {
            return "用户";
        } else if (requestURI.contains("problem") || requestURI.contains("oj")) {
            return "题目";
        } else if (requestURI.contains("solution")) {
            return "题解";
        } else if (requestURI.contains("announcement")) {
            return "公告";
        } else if (requestURI.contains("feedback")) {
            return "反馈";
        } else if (requestURI.contains("product")) {
            return "商品";
        } else if (requestURI.contains("coin")) {
            return "金币";
        } else if (requestURI.contains("order")) {
            return "订单";
        } else if (requestURI.contains("config")) {
            return "系统配置";
        } else if (requestURI.contains("role")) {
            return "角色";
        } else if (requestURI.contains("password")) {
            return "密码";
        } else if (requestURI.contains("audit")) {
            return "审核";
        } else if (requestURI.contains("content")) {
            return "内容";
        }
        return null;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
