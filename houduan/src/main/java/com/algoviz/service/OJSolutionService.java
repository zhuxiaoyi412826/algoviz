package com.algoviz.service;

import com.algoviz.audit.AuditDetectService;
import com.algoviz.audit.AuditLogEntry;
import com.algoviz.audit.AuditLogService;
import com.algoviz.audit.DfaTrie;
import com.algoviz.audit.DetectResult;
import com.algoviz.audit.SensitiveWordService;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.OJSolution;
import com.algoviz.entity.OJSolutionLike;
import com.algoviz.entity.Submission;
import com.algoviz.mapper.OJSolutionLikeMapper;
import com.algoviz.mapper.OJSolutionMapper;
import com.algoviz.mapper.SubmissionMapper;
import com.algoviz.common.util.DebugLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * OJ 题解服务：
 * - 发布题解：先检测（BLOCK 拒绝 / MEDIUM pending / LOW 放行），再入库 + 审计日志
 * - 查询题解：返回屏蔽版（敏感词 *** 替换），原文只在用户编辑自己的题解时返回
 * - 点赞：去重表，toggle 模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OJSolutionService {

    private final OJSolutionMapper solutionMapper;
    private final OJSolutionLikeMapper likeMapper;
    private final SubmissionMapper submissionMapper;
    private final AuditDetectService auditDetectService;
    private final AuditLogService auditLogService;
    private final SensitiveWordService wordService;

    // ==================== 发布题解 ====================

    @Transactional
    public PublishResult publish(OJSolution s) {
        // 调试日志
        DebugLogger.log("OJSolutionService.publish", "userId=" + s.getUserId() + 
            ", username=" + s.getUsername() + ", avatar=" + s.getAvatar());
        
        // 检测内容：标题 + 思路 + 解题过程 + 代码
        String fullText = safe(s.getTitle()) + "\n" + safe(s.getIdea()) + "\n"
                + safe(s.getProcess()) + "\n" + safe(s.getCode());
        DetectResult d = auditDetectService.detect("OJ_SOL", safe(s.getCodeLang()),
                s.getTitle(), fullText);

        // BLOCK（高危）：拒绝发布，不入库
        if ("BLOCK".equals(d.getPreCheck())) {
            logAudit(s, d, "blocked");
            return new PublishResult(false, "内容包含违规信息，请修改后重新发布",
                    d.getRiskLevel(), d.getAuditStatus(), null);
        }

        // 查是否已 AC 该题
        boolean passed = checkUserPassed(s.getProblemId(), s.getUserId());
        s.setIsPassed(passed ? 1 : 0);

        // 查是否已有题解（同一用户同一题只允许一篇，更新而非新增）
        OJSolution existing = solutionMapper.selectByUserAndProblem(s.getUserId(), s.getProblemId());
        if (existing != null) {
            s.setId(existing.getId());
            s.setAuditStatus(d.getAuditStatus());
            s.setRiskLevel(d.getRiskLevel());
            s.setDetectSummary(buildSummary(d));
            solutionMapper.updateById(s);
            logAudit(s, d, d.getAuditStatus());
            return new PublishResult(true, "题解已更新",
                    d.getRiskLevel(), d.getAuditStatus(), existing.getId());
        }

        // 新增
        s.setLikeCount(0);
        s.setViewCount(0);
        s.setCommentCount(0);
        s.setIsFeatured(0);
        s.setAuditStatus(d.getAuditStatus());
        s.setRiskLevel(d.getRiskLevel());
        s.setDetectSummary(buildSummary(d));
        s.setStatus("PUBLISHED");
        
        // 调试日志 - 插入前
        DebugLogger.log("OJSolutionService.publish", "insert前: userId=" + s.getUserId() + 
            ", username=" + s.getUsername() + ", avatar=" + s.getAvatar() +
            ", problemId=" + s.getProblemId());
        
        solutionMapper.insert(s);
        
        // 调试日志 - 插入后
        DebugLogger.log("OJSolutionService.publish", "insert后: id=" + s.getId());
        
        logAudit(s, d, d.getAuditStatus());
        return new PublishResult(true, "题解发布成功",
                d.getRiskLevel(), d.getAuditStatus(), s.getId());
    }

    /** 查用户是否已 AC 该题 */
    private boolean checkUserPassed(Long problemId, Long userId) {
        try {
            List<Submission> subs = submissionMapper.findByUserIdAndProblemId(userId, problemId);
            if (subs == null) return false;
            return subs.stream().anyMatch(s -> "AC".equalsIgnoreCase(s.getStatus()));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 查询（屏蔽版） ====================

    /** 用户侧：题解列表（精简版，无需DFA屏蔽，不查询大字段） */
    public PageResult<OJSolution> listPublished(Long problemId, int page, int pageSize) {
        int total = solutionMapper.countPublished(problemId);
        // 性能优化：走 selectPublishedBrief 只查列表需要的字段（跳过 idea/process/code MEDIUMTEXT）
        // 列表页只显示标题、格式、点赞数等元信息，不需要全文也不需要 DFA 屏蔽
        List<OJSolution> list = solutionMapper.selectPublishedBrief(problemId, (page - 1) * pageSize, pageSize);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 题解详情（屏蔽版） */
    public OJSolution getDetail(Long id, Long currentUserId) {
        OJSolution s = solutionMapper.selectById(id);
        if (s == null || "DELETED".equals(s.getStatus())) return null;
        solutionMapper.incrementViewCount(id);
        s.setViewCount((s.getViewCount() != null ? s.getViewCount() : 0) + 1);
        applyMask(s);
        if (currentUserId != null) {
            s.setLiked(checkLiked(currentUserId, "SOLUTION", id));
        }
        return s;
    }

    /** 用户编辑页：返回原文（仅本人） */
    public OJSolution getMySolution(Long userId, Long problemId) {
        OJSolution s = solutionMapper.selectByUserAndProblem(userId, problemId);
        return s; // 原文，不做屏蔽
    }

    // ==================== 点赞 ====================

    @Transactional
    public boolean toggleLike(Long userId, String targetType, Long targetId) {
        OJSolutionLike existing = likeMapper.selectByUserAndTarget(userId, targetType, targetId);
        if (existing != null) {
            likeMapper.deleteByUserAndTarget(userId, targetType, targetId);
            if ("SOLUTION".equals(targetType)) {
                solutionMapper.incrementLikeCount(targetId, -1);
            }
            return false; // 取消点赞
        } else {
            OJSolutionLike l = new OJSolutionLike();
            l.setUserId(userId);
            l.setTargetType(targetType);
            l.setTargetId(targetId);
            try {
                likeMapper.insert(l);
                if ("SOLUTION".equals(targetType)) {
                    solutionMapper.incrementLikeCount(targetId, 1);
                }
                return true; // 点赞成功
            } catch (Exception e) {
                // 唯一键冲突 = 已点赞，忽略
                return false;
            }
        }
    }

    private boolean checkLiked(Long userId, String targetType, Long targetId) {
        return likeMapper.selectByUserAndTarget(userId, targetType, targetId) != null;
    }

    // ==================== 后台审核 ====================

    public PageResult<OJSolution> adminList(String keyword, String auditStatus, int page, int pageSize) {
        int total = solutionMapper.countByPage(keyword, auditStatus);
        List<OJSolution> list = solutionMapper.selectByPage(keyword, auditStatus,
                (page - 1) * pageSize, pageSize);
        // 后台返回原文，不做屏蔽
        return PageResult.of(list, total, page, pageSize);
    }

    @Transactional
    public boolean adminUpdateStatus(Long id, String status) {
        return solutionMapper.updateStatus(id, status) > 0;
    }

    @Transactional
    public boolean adminPassAudit(Long id) {
        solutionMapper.updateAuditStatus(id, "passed", "NONE", null);
        solutionMapper.updateStatus(id, "PUBLISHED");
        return true;
    }

    @Transactional
    public boolean adminRejectAudit(Long id) {
        solutionMapper.updateAuditStatus(id, "rejected", "NONE", null);
        solutionMapper.updateStatus(id, "HIDDEN");
        return true;
    }

    // ==================== 屏蔽工具 ====================

    /** 对题解字段做敏感词屏蔽替换 */
    private void applyMask(OJSolution s) {
        if (s == null) return;
        s.setMaskIdea(maskContent(s.getIdea()));
        s.setMaskProcess(maskContent(s.getProcess()));
        s.setMaskCode(maskContent(s.getCode()));
    }

    /** 用 DFA 命中位置做 *** 替换（等长，保留原始字符数） */
    private String maskContent(String text) {
        if (text == null || text.isEmpty()) return text;
        List<DfaTrie.Hit> hits = wordService.getTrie().matchAll(text);
        if (hits.isEmpty()) return text;
        char[] arr = text.toCharArray();
        for (DfaTrie.Hit h : hits) {
            int len = h.word().length();
            for (int i = h.start(); i < h.start() + len && i < arr.length; i++) {
                arr[i] = '*';
            }
        }
        return new String(arr);
    }

    private String buildSummary(DetectResult d) {
        if (d.getHits() == null || d.getHits().isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (DetectResult.HitDetail h : d.getHits()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(h.getRuleName()).append("(").append(h.getHitContent()).append(")");
        }
        return sb.length() > 1000 ? sb.substring(0, 1000) : sb.toString();
    }

    private void logAudit(OJSolution s, DetectResult d, String status) {
        try {
            AuditLogEntry e = new AuditLogEntry();
            e.setSubmitId(AuditLogService.nextSubmitId("sol"));
            e.setUserId(s.getUserId());
            e.setProblemId(s.getProblemId());
            e.setContentType("OJ_SOL");
            e.setLanguage(s.getCodeLang());
            e.setTitle(s.getTitle());
            e.setContent(truncate(safe(s.getIdea()) + " " + safe(s.getCode()), 500));
            e.setRiskLevel(d.getRiskLevel());
            e.setTotalScore(d.getTotalScore());
            e.setHitDetails(d.getHits());
            e.setPreCheck(d.getPreCheck());
            e.setAuditStatus(status);
            e.setSubmitTime(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            auditLogService.log(e);
        } catch (Exception ex) {
            log.warn("[oj-solution] 审计日志写入失败: {}", ex.getMessage());
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    // ==================== 结果 DTO ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PublishResult {
        private boolean success;
        private String message;
        private String riskLevel;
        private String auditStatus;
        private Long solutionId;
    }
}
