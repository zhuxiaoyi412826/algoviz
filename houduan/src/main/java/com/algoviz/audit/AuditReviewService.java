package com.algoviz.audit;

import com.algoviz.integration.es.EsAuditClient;
import com.algoviz.task.AuditPullTask;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.ContentAuditRecord;
import com.algoviz.mapper.ContentAuditRecordMapper;
import com.algoviz.mapper.InterviewProblemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人工审核服务：
 * - 待审列表：读 Redis audit:pending 队列
 * - 审核完毕：写 MySQL content_audit_record + 回写 ES audit_status + 驳回时题目下架
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditReviewService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ContentAuditRecordMapper recordMapper;
    private final InterviewProblemMapper problemMapper;
    private final EsAuditClient esClient;
    private final AuditPullTask pullTask;

    /** 待审核列表（Redis 队列分页） */
    public PageResult<AuditLogEntry> pendingList(int page, int pageSize) {
        long total = pullTask.pendingCount();
        List<AuditLogEntry> items = new ArrayList<>();
        if (total > 0) {
            try {
                long start = (long) (page - 1) * pageSize;
                long end = start + pageSize - 1;
                List<String> raw = redis.opsForList().range(AuditPullTask.PENDING_KEY, start, end);
                if (raw != null) {
                    for (String s : raw) {
                        items.add(objectMapper.readValue(s, AuditLogEntry.class));
                    }
                }
            } catch (Exception e) {
                log.warn("[audit] 读取待审队列失败: {}", e.getMessage());
            }
        }
        return PageResult.of(items, total, page, pageSize);
    }

    /**
     * 审核一条
     *
     * @param result pass / reject
     */
    public String review(String submitId, String result, String remark, String auditorId) {
        if (!"pass".equals(result) && !"reject".equals(result)) {
            throw new IllegalArgumentException("result 仅支持 pass/reject");
        }
        // 1. 从 Redis 队列移除
        AuditLogEntry entry = null;
        try {
            List<String> all = redis.opsForList().range(AuditPullTask.PENDING_KEY, 0, -1);
            if (all != null) {
                for (String s : all) {
                    AuditLogEntry e = objectMapper.readValue(s, AuditLogEntry.class);
                    if (submitId.equals(e.getSubmitId())) {
                        entry = e;
                        redis.opsForList().remove(AuditPullTask.PENDING_KEY, 1, s);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[audit] Redis 队列操作失败: {}", e.getMessage());
        }
        if (entry == null) {
            // 队列没有，尝试从已落库记录补审（幂等）
            ContentAuditRecord exist = recordMapper.selectBySubmitId(submitId);
            if (exist == null) throw new IllegalArgumentException("待审核项不存在: " + submitId);
            entry = new AuditLogEntry();
            entry.setSubmitId(exist.getSubmitId());
            entry.setUserId(exist.getUserId());
            entry.setProblemId(exist.getProblemId());
            entry.setProblemNo(exist.getProblemNo());
            entry.setContentType(exist.getContentType());
            entry.setLanguage(exist.getLanguage());
            entry.setRiskLevel(exist.getRiskLevel());
            entry.setContent(exist.getContentSnapshot());
            entry.setEsDocId(exist.getEsDocId());
        }

        // 2. 审核结果落 MySQL
        ContentAuditRecord r = new ContentAuditRecord();
        r.setSubmitId(entry.getSubmitId());
        r.setUserId(entry.getUserId());
        r.setProblemId(entry.getProblemId());
        r.setProblemNo(entry.getProblemNo());
        r.setContentType(entry.getContentType());
        r.setLanguage(entry.getLanguage());
        r.setRiskLevel(entry.getRiskLevel());
        r.setTotalScore(entry.getTotalScore());
        r.setContentSnapshot(entry.getContent());
        r.setPreCheckStatus(entry.getPreCheck());
        r.setAuditStatus(result);
        r.setAuditRemark(remark);
        r.setAuditorId(auditorId);
        r.setAuditTime(LocalDateTime.now());
        r.setEsDocId(entry.getEsDocId());
        try {
            r.setHitDetails(entry.getHitDetails() == null ? null
                    : objectMapper.writeValueAsString(entry.getHitDetails()));
        } catch (Exception ignored) {
        }
        try {
            r.setSubmitTime(entry.getSubmitTime() == null ? null
                    : LocalDateTime.parse(entry.getSubmitTime(),
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (Exception ignored) {
        }
        recordMapper.insert(r);

        // 3. 回写 ES audit_status（容忍失败）
        if (entry.getEsIndex() != null && entry.getEsDocId() != null) {
            esClient.updateAuditStatus(entry.getEsIndex(), entry.getEsDocId(), result);
        }

        // 4. 驳回 → 题目下架（内容处置闭环）
        if ("reject".equals(result) && entry.getProblemId() != null) {
            problemMapper.updateStatus(entry.getProblemId(), "INACTIVE", auditorId);
            log.info("[audit] 题目 {} 已因审核驳回下架", entry.getProblemId());
        }
        return ("pass".equals(result) ? "审核通过，记录已入库" : "已驳回，关联题目已下架");
    }

    /** 审核记录（MySQL） */
    public PageResult<ContentAuditRecord> records(String auditStatus, String riskLevel, String keyword,
                                                  int page, int pageSize) {
        int total = recordMapper.countByPage(auditStatus, riskLevel, keyword);
        List<ContentAuditRecord> list = recordMapper.selectByPage(auditStatus, riskLevel, keyword,
                (page - 1) * pageSize, pageSize);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 统计 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("pendingQueue", pullTask.pendingCount());
        long total = recordMapper.countAll();
        m.put("totalRecords", total);
        Map<String, Long> byStatus = new java.util.HashMap<>();
        for (Map<String, Object> row : recordMapper.countByStatus()) {
            Object s = row.get("status");
            Object c = row.get("cnt");
            byStatus.put(s == null ? "unknown" : s.toString(),
                    c == null ? 0L : Long.parseLong(c.toString()));
        }
        m.put("byStatus", byStatus);
        return m;
    }

    /** BLOCK 拦截记录直接落库（不走人工审核） */
    public void recordBlocked(AuditLogEntry e, DetectResult d) {
        try {
            ContentAuditRecord r = new ContentAuditRecord();
            r.setSubmitId(e.getSubmitId());
            r.setUserId(e.getUserId());
            r.setProblemId(e.getProblemId());
            r.setProblemNo(e.getProblemNo());
            r.setContentType(e.getContentType());
            r.setLanguage(e.getLanguage());
            r.setRiskLevel(d.getRiskLevel());
            r.setTotalScore(d.getTotalScore());
            r.setContentSnapshot(e.getContent());
            r.setPreCheckStatus("BLOCK");
            r.setAuditStatus("blocked");
            r.setAuditRemark("高危命中，系统自动拦截");
            r.setHitDetails(objectMapper.writeValueAsString(d.getHits()));
            r.setSubmitTime(LocalDateTime.now());
            recordMapper.insert(r);
        } catch (Exception ex) {
            log.warn("[audit] BLOCK 记录落库失败: {}", ex.getMessage());
        }
    }
}
