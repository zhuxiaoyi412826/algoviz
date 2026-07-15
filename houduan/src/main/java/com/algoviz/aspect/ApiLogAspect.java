package com.algoviz.aspect;

import com.algoviz.entity.ApiLog;
import com.algoviz.mapper.ApiLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
public class ApiLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogAspect.class);
    
    @Autowired
    private ApiLogMapper apiLogMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("execution(* com.algoviz.controller..*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        String apiPath = request != null ? request.getRequestURI() : "";
        String httpMethod = request != null ? request.getMethod() : "";
        String clientIp = request != null ? getClientIp(request) : "";
        String userId = request != null ? request.getHeader("X-User-Id") : null;
        
        Object[] args = joinPoint.getArgs();
        String requestBody = "";
        try {
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg != null && !(arg instanceof HttpServletRequest) && !(arg instanceof org.springframework.web.multipart.MultipartFile)) {
                        requestBody = objectMapper.writeValueAsString(arg);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            requestBody = "Cannot serialize request body";
        }

        ApiLog apiLog = new ApiLog();
        apiLog.setId(UUID.randomUUID().toString());
        apiLog.setApiPath(apiPath);
        apiLog.setHttpMethod(httpMethod);
        apiLog.setClientIp(clientIp);
        apiLog.setUserId(userId);
        apiLog.setRequestBody(truncate(requestBody, 2000));

        Object result = null;
        int statusCode = 200;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            statusCode = 500;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            
            apiLog.setStatusCode(statusCode);
            apiLog.setResponseTime(responseTime);
            apiLog.setErrorMessage(truncate(errorMessage, 500));
            apiLog.setCreateTime(java.time.LocalDateTime.now().toString());

            try {
                apiLogMapper.insert(apiLog);
            } catch (Exception e) {
                logger.error("Failed to save API log", e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
