package com.algoviz.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计日志输出：
 * SLF4J logger "AUDIT" → D:/rizi/info.log（logback 现有 appender）
 * → Fluentd tail 采集 → ES algoviz-logs-YYYY.MM.DD
 * 消息格式：AUDIT|{单行JSON}
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() / 1000);

    /** 生成提交ID：q_时间戳_随机 */
    public static String nextSubmitId(String prefix) {
        return prefix + "_" + SEQ.incrementAndGet() + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 输出一条审计日志（单行 JSON，Fluentd multiline 正则安全）
     */
    public void log(AuditLogEntry e) {
        try {
            if (e.getSubmitTime() == null) {
                e.setSubmitTime(LocalDateTime.now().format(FMT));
            }
            String json = objectMapper.writeValueAsString(e);
            // 保证单行：Jackson 默认转义换行，安全
            AUDIT.info("AUDIT|{}", json);
        } catch (Exception ex) {
            AUDIT.warn("AUDIT 日志序列化失败: {}", ex.getMessage());
        }
    }
}
