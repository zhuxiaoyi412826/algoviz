package com.algoviz.task;

import com.algoviz.audit.AuditLogEntry;
import com.algoviz.integration.es.EsAuditClient;
import com.algoviz.mapper.ContentAuditRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Task 定时任务：
 * ES(algoviz-logs-*) → 拉取 auditStatus=pending 的审计日志 → 去重 → 推送 Redis 待审核队列
 *
 * Redis 结构：
 *   audit:pending  List  待审核队列（元素为 AuditLogEntry JSON）
 *   audit:pushed   Set   已推送过的 submitId（防重复推送）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditPullTask {

    public static final String PENDING_KEY = "audit:pending";
    public static final String PUSHED_KEY = "audit:pushed";

    private final EsAuditClient esClient;
    private final StringRedisTemplate redis;
    private final ContentAuditRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    @Value("${audit.pull-batch-size:200}")
    private int batchSize;

    /**
     * 默认每 60 秒执行一次（audit.pull-interval-ms 可配），启动后 20 秒先跑一轮
     */
    @Scheduled(fixedDelayString = "${audit.pull-interval-ms:60000}", initialDelay = 20000)
    public void pull() {
        // 检查 ES 是否可用，不可用时静默跳过
        if (!esClient.available()) {
            log.debug("[audit] ES 不可用，跳过本轮拉取");
            return;
        }
        try {
            String summary = doPull();
            if (!summary.isEmpty()) {
                log.info("[audit] 定时拉取完成: {}", summary);
            }
        } catch (Exception e) {
            log.warn("[audit] 定时拉取失败(下轮重试): {}", e.getMessage());
        }
    }

    /** 手动触发（管理页按钮） */
    public String doPull() throws Exception {
        List<AuditLogEntry> logs = esClient.searchAuditLogs(batchSize);
        int pushed = 0;
        int skipped = 0;
        for (AuditLogEntry e : logs) {
            if (!"pending".equals(e.getAuditStatus())) continue;
            // 去重1：Redis 已推送集合
            try {
                Boolean isMember = redis.opsForSet().isMember(PUSHED_KEY, e.getSubmitId());
                if (Boolean.TRUE.equals(isMember)) {
                    skipped++;
                    continue;
                }
            } catch (Exception rex) {
                return "Redis 不可用，本轮跳过: " + rex.getMessage();
            }
            // 去重2：MySQL 已有审核记录（含已审核完）
            if (recordMapper.selectBySubmitId(e.getSubmitId()) != null) {
                skipped++;
                try {
                    redis.opsForSet().add(PUSHED_KEY, e.getSubmitId());
                } catch (Exception ignored) {
                }
                continue;
            }
            // 推入待审核队列
            redis.opsForList().rightPush(PENDING_KEY, objectMapper.writeValueAsString(e));
            redis.opsForSet().add(PUSHED_KEY, e.getSubmitId());
            pushed++;
        }
        if (pushed > 0) {
            log.info("[audit] 推送 {} 条待审核(pending)任务到 Redis 队列, 跳过已处理 {} 条", pushed, skipped);
            return "ES拉取 " + logs.size() + " 条审计日志，新推送 " + pushed + " 条待审核，跳过 " + skipped + " 条重复";
        }
        return "";
    }

    /** 队列当前长度 */
    public long pendingCount() {
        try {
            Long n = redis.opsForList().size(PENDING_KEY);
            return n == null ? 0 : n;
        } catch (Exception e) {
            return -1;
        }
    }
}
