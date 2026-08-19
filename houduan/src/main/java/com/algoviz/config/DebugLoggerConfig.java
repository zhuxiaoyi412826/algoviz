package com.algoviz.config;

import com.algoviz.util.DebugLogger;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * 调试日志配置
 * 应用启动时初始化日志系统，关闭时清理
 */
@Configuration
public class DebugLoggerConfig implements ApplicationListener<ApplicationReadyEvent> {
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 应用就绪时初始化日志
        DebugLogger.init();
    }
    
    @PreDestroy
    public void onShutdown() {
        // 应用关闭时清理日志
        DebugLogger.close();
    }
}
