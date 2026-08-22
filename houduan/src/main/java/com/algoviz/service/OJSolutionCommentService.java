package com.algoviz.service;

import com.algoviz.audit.AuditDetectService;
import com.algoviz.audit.AuditLogEntry;
import com.algoviz.audit.AuditLogService;
import com.algoviz.audit.DfaTrie;
import com.algoviz.audit.DetectResult;
import com.algoviz.audit.SensitiveWordService;
import com.algoviz.dto.interview.IdCount;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.OJSolutionComment;
import com.algoviz.entity.OJSolutionLike;
import com.algoviz.mapper.OJSolutionCommentMapper;
import com.algoviz.mapper.OJSolutionLikeMapper;
import com.algoviz.mapper.OJSolutionMapper;
import com.algoviz.util.DebugLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OJ 题解评论服务：
 * - 评论发布：敏感词检测 → BLOCK 拒绝 / MEDIUM pending / LOW 放行
 * - 多级评论：parent_id + root_id 结构
 * - 屏蔽返回：maskContent 字段 ***
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OJSolutionCommentService {

    private final OJSolutionCommentMapper commentMapper;
    private final OJSolutionLikeMapper likeMapper;
    private final OJSolutionMapper solutionMapper;
    private final AuditDetectService auditDetectService;
    private final AuditLogService auditLogService;
    private final SensitiveWordService wordService;

    // ==================== DFA 屏蔽缓存（避免对相同评论内容反复扫描） ====================
    // LRU 风格：使用 ConcurrentHashMap，控制最多 2000 条；评论内容不会太长，几千条只占 MB 级别
    private static final int MASK_CACHE_MAX = 2000;
    private final Map<String, String> maskCache = new ConcurrentHashMap<>(256);
    private final Object maskCacheLock = new Object();

    private String cachedMaskContent(String text) {
        if (text == null || text.isEmpty()) return text;
        // 短文本直接用内容作 key；评论普遍很短，hash 无意义
        String hit = maskCache.get(text);
        if (hit != null) return hit;
        String masked = doMaskContent(text);
        // 放入缓存，超过阈值则清空一半（简单 LRU 替代方案）
        if (maskCache.size() >= MASK_CACHE_MAX) {
            synchronized (maskCacheLock) {
                if (maskCache.size() >= MASK_CACHE_MAX) {
                    Iterator<Map.Entry<String, String>> it = maskCache.entrySet().iterator();
                    int drop = MASK_CACHE_MAX >> 1;
                    while (it.hasNext() && drop-- > 0) {
                        it.next();
                        it.remove();
                    }
                }
            }
        }
        maskCache.put(text, masked);
        return masked;
    }

    // ==================== 发布评论 ====================

    @Transactional
    public PublishResult publish(OJSolutionComment c) {
        // 调试日志
        DebugLogger.log("OJSolutionCommentService.publish", "userId=" + c.getUserId() + 
            ", username=" + c.getUsername() + ", avatar=" + c.getAvatar() +
            ", problemId=" + c.getProblemId() + ", solutionId=" + c.getSolutionId());
        
        // 敏感词检测
        DetectResult d = auditDetectService.detect("OJ_COMMENT", "ALL",
                null, c.getContent());

        if ("BLOCK".equals(d.getPreCheck())) {
            logAudit(c, d, "blocked");
            return new PublishResult(false, "评论包含违规内容，请修改后重新发布",
                    d.getRiskLevel(), d.getAuditStatus(), null);
        }

        // 设置 root_id：顶层评论 root_id=自身（insert 后回填），子评论 root_id=父评论的 root_id
        if (c.getParentId() == null || c.getParentId() == 0) {
            c.setParentId(0L);
        }
        c.setLikeCount(0);
        c.setAuditStatus(d.getAuditStatus());
        c.setRiskLevel(d.getRiskLevel());
        c.setDetectSummary(buildSummary(d));
        c.setStatus("PUBLISHED");
        commentMapper.insert(c);

        // 顶层评论：回填 root_id = 自身 id
        if (c.getParentId() == 0) {
            commentMapper.updateRootId(c.getId(), c.getId());
        } else {
            // 子评论：查父评论的 root_id
            OJSolutionComment parent = commentMapper.selectById(c.getParentId());
            if (parent != null) {
                commentMapper.updateRootId(c.getId(), parent.getRootId() != null ? parent.getRootId() : parent.getId());
            }
        }

        // 题解评论数 +1
        solutionMapper.incrementCommentCount(c.getSolutionId(), 1);

        logAudit(c, d, d.getAuditStatus());
        return new PublishResult(true, "评论发布成功",
                d.getRiskLevel(), d.getAuditStatus(), c.getId());
    }

    // ==================== 查询（屏蔽版） ====================

    /**
     * 顶层评论列表（分页）。
     * 性能核心优化：
     *  - 回复数 N+1 → 一次性批量 GROUP BY 查询（从 21 次 SQL 降到 3 次）
     *  - DFA 屏蔽走本地缓存，相同评论内容不重复扫描 DFA Trie
     */
    public PageResult<OJSolutionComment> listTopLevel(Long solutionId, int page, int pageSize) {
        int total = commentMapper.countTopLevel(solutionId);
        List<OJSolutionComment> list = commentMapper.selectTopLevel(solutionId,
                (page - 1) * pageSize, pageSize);

        // --- 批量查回复数：1 次 SQL 取代 pageSize 次 COUNT ---
        if (!list.isEmpty()) {
            List<Long> rootIds = list.stream()
                    .map(OJSolutionComment::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            Map<Long, Integer> replyCountMap = new HashMap<>(rootIds.size());
            try {
                List<IdCount> idCounts = commentMapper.countRepliesByRootIds(rootIds);
                if (idCounts != null) {
                    for (IdCount ic : idCounts) {
                        if (ic != null && ic.getId() != null) {
                            replyCountMap.put(ic.getId(), ic.getCnt() == null ? 0 : ic.getCnt());
                        }
                    }
                }
            } catch (Exception e) {
                // 兜底：退化到逐行查询（避免批量方法异常时整页不可用）
                log.warn("[oj-comment] 批量查询回复数失败，退化到 N+1: {}", e.getMessage());
                list.forEach(c -> replyCountMap.put(c.getId(), commentMapper.countReplies(c.getId())));
            }
            list.forEach(c -> {
                Integer rc = replyCountMap.get(c.getId());
                c.setReplyCount(rc == null ? 0 : rc);
                c.setMaskContent(cachedMaskContent(c.getContent())); // 缓存 DFA 扫描
            });
        }
        return PageResult.of(list, total, page, pageSize);
    }

    /** 子评论列表 */
    public PageResult<OJSolutionComment> listReplies(Long rootId, int page, int pageSize) {
        int total = commentMapper.countReplies(rootId);
        List<OJSolutionComment> list = commentMapper.selectReplies(rootId,
                (page - 1) * pageSize, pageSize);
        list.forEach(c -> c.setMaskContent(cachedMaskContent(c.getContent())));
        return PageResult.of(list, total, page, pageSize);
    }

    // ==================== 点赞 ====================

    @Transactional
    public boolean toggleLike(Long userId, Long commentId) {
        OJSolutionLike existing = likeMapper.selectByUserAndTarget(userId, "COMMENT", commentId);
        if (existing != null) {
            likeMapper.deleteByUserAndTarget(userId, "COMMENT", commentId);
            commentMapper.incrementLikeCount(commentId, -1);
            return false;
        } else {
            OJSolutionLike l = new OJSolutionLike();
            l.setUserId(userId);
            l.setTargetType("COMMENT");
            l.setTargetId(commentId);
            try {
                likeMapper.insert(l);
                commentMapper.incrementLikeCount(commentId, 1);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    // ==================== 后台审核 ====================

    public PageResult<OJSolutionComment> adminList(String keyword, String auditStatus, int page, int pageSize) {
        int total = commentMapper.countByPage(keyword, auditStatus);
        List<OJSolutionComment> list = commentMapper.selectByPage(keyword, auditStatus,
                (page - 1) * pageSize, pageSize);
        return PageResult.of(list, total, page, pageSize);
    }

    @Transactional
    public boolean adminPassAudit(Long id) {
        commentMapper.updateAuditStatus(id, "passed", "NONE", null);
        commentMapper.updateStatus(id, "PUBLISHED");
        return true;
    }

    @Transactional
    public boolean adminRejectAudit(Long id) {
        commentMapper.updateAuditStatus(id, "rejected", "NONE", null);
        commentMapper.updateStatus(id, "HIDDEN");
        return true;
    }

    // ==================== 屏蔽工具 ====================

    /** 真正执行 DFA 扫描并做 *** 屏蔽（外层通过 cachedMaskContent + 内存缓存调用） */
    private String doMaskContent(String text) {
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

    private void logAudit(OJSolutionComment c, DetectResult d, String status) {
        try {
            AuditLogEntry e = new AuditLogEntry();
            e.setSubmitId(AuditLogService.nextSubmitId("cmt"));
            e.setUserId(c.getUserId());
            e.setProblemId(c.getProblemId());
            e.setContentType("OJ_COMMENT");
            e.setLanguage("ALL");
            e.setTitle(null);
            e.setContent(truncate(c.getContent(), 500));
            e.setRiskLevel(d.getRiskLevel());
            e.setTotalScore(d.getTotalScore());
            e.setHitDetails(d.getHits());
            e.setPreCheck(d.getPreCheck());
            e.setAuditStatus(status);
            e.setSubmitTime(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            auditLogService.log(e);
        } catch (Exception ex) {
            log.warn("[oj-comment] 审计日志写入失败: {}", ex.getMessage());
        }
    }

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
        private Long commentId;
    }
}
