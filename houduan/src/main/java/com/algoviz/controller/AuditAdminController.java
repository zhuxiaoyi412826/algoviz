package com.algoviz.controller;

import com.algoviz.audit.*;
import com.algoviz.task.AuditPullTask;
import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.ContentAuditRecord;
import com.algoviz.entity.DangerousCodeRule;
import com.algoviz.entity.SensitiveWord;
import com.algoviz.entity.SensitiveWordVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关键词屏蔽审核系统 - 管理接口
 * 路径前缀：/api/audit
 */
@Tag(name = "内容审核-关键词屏蔽", description = "敏感词管理/版本/危险代码规则/人工审核")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditAdminController {

    private final SensitiveWordService wordService;
    private final DangerousCodeRuleService ruleService;
    private final AuditDetectService detectService;
    private final AuditPullTask pullTask;
    private final AuditReviewService reviewService;

    private static String adminId() {
        try {
            Object u = com.algoviz.common.util.UserContextHolder.get();
            if (u != null) {
                Object v = u.getClass().getMethod("getId").invoke(u);
                if (v != null) return v.toString();
            }
        } catch (Throwable ignored) {}
        return "admin";
    }

    // ==================== 敏感词 ====================

    @Operation(summary = "敏感词分页列表")
    @GetMapping("/words")
    public InterviewResponse<PageResult<SensitiveWord>> words(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return InterviewResponse.ok(wordService.list(keyword, category, level, page, pageSize));
    }

    @Operation(summary = "新增/修改敏感词")
    @PostMapping("/words")
    public InterviewResponse<SensitiveWord> saveWord(@RequestBody SensitiveWord w) {
        try {
            return InterviewResponse.ok("保存成功", wordService.save(w));
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "批量新增敏感词")
    @PostMapping("/words/batch")
    public InterviewResponse<Integer> saveWords(@RequestBody BatchWordsBody body) {
        List<SensitiveWord> words = body.getWords();
        if (words == null || words.isEmpty()) return InterviewResponse.fail(400, "缺少 words 字段");
        return InterviewResponse.ok("成功导入 " + wordService.saveBatch(words) + " 条", words.size());
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/words/{id}")
    public InterviewResponse<Void> deleteWord(@PathVariable Long id) {
        return wordService.delete(id) ? InterviewResponse.ok("删除成功")
                : InterviewResponse.fail(404, "词不存在");
    }

    @Operation(summary = "批量删除敏感词")
    @DeleteMapping("/words/batch")
    public InterviewResponse<Void> deleteWords(@RequestBody DeleteWordsBody body) {
        List<Long> ids = body.getIds();
        return InterviewResponse.ok("成功删除 " + wordService.deleteBatch(ids) + " 条");
    }

    @Operation(summary = "刷新词库 Redis 缓存")
    @PostMapping("/words/refresh-cache")
    public InterviewResponse<String> refreshCache() {
        return InterviewResponse.ok(wordService.refreshCache());
    }

    // ==================== 版本管理 ====================

    @Operation(summary = "发布当前词库为新版本")
    @PostMapping("/versions/publish")
    public InterviewResponse<SensitiveWordVersion> publish(@RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        return InterviewResponse.ok("版本发布成功", wordService.publishVersion(remark, adminId()));
    }

    @Operation(summary = "版本列表")
    @GetMapping("/versions")
    public InterviewResponse<List<SensitiveWordVersion>> versions() {
        return InterviewResponse.ok(wordService.versions());
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/versions/{versionNo}/rollback")
    public InterviewResponse<String> rollback(@PathVariable int versionNo) {
        try {
            return InterviewResponse.ok(wordService.rollback(versionNo));
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(404, e.getMessage());
        }
    }

    // ==================== 危险代码规则 ====================

    @Operation(summary = "危险代码规则分页列表")
    @GetMapping("/rules")
    public InterviewResponse<PageResult<DangerousCodeRule>> rules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return InterviewResponse.ok(ruleService.list(keyword, language, riskLevel, page, pageSize));
    }

    @Operation(summary = "新增/修改危险代码规则")
    @PostMapping("/rules")
    public InterviewResponse<DangerousCodeRule> saveRule(@RequestBody DangerousCodeRule r) {
        try {
            return InterviewResponse.ok("保存成功", ruleService.save(r));
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除危险代码规则")
    @DeleteMapping("/rules/{id}")
    public InterviewResponse<Void> deleteRule(@PathVariable Long id) {
        return ruleService.delete(id) ? InterviewResponse.ok("删除成功")
                : InterviewResponse.fail(404, "规则不存在");
    }

    // ==================== 检测测试 ====================

    @Operation(summary = "手动测试检测（调试用）")
    @PostMapping("/detect")
    public InterviewResponse<DetectResult> detect(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");
        String language = body.getOrDefault("language", "ALL");
        String contentType = body.getOrDefault("contentType", "QUESTION");
        return InterviewResponse.ok(detectService.detect(contentType, language, title, content));
    }

    // ==================== 待审核 + 人工审核 ====================

    @Operation(summary = "待审核列表（Redis 队列）")
    @GetMapping("/pending")
    public InterviewResponse<PageResult<AuditLogEntry>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return InterviewResponse.ok(reviewService.pendingList(page, pageSize));
    }

    @Operation(summary = "手动触发 ES 拉取（不必等定时任务）")
    @PostMapping("/pending/pull")
    public InterviewResponse<String> manualPull() {
        try {
            String r = pullTask.doPull();
            return InterviewResponse.ok(r.isEmpty() ? "本轮无新增待审核项" : r);
        } catch (Exception e) {
            return InterviewResponse.fail(500, "拉取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "人工审核（pass 通过 / reject 驳回并下架）")
    @PostMapping("/review")
    public InterviewResponse<String> review(@RequestBody Map<String, String> body) {
        String submitId = body.get("submitId");
        String result = body.get("result");
        String remark = body.get("remark");
        if (submitId == null || result == null) {
            return InterviewResponse.fail(400, "缺少 submitId / result");
        }
        try {
            return InterviewResponse.ok(reviewService.review(submitId, result, remark, adminId()));
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(404, e.getMessage());
        }
    }

    @Operation(summary = "审核记录（MySQL）")
    @GetMapping("/records")
    public InterviewResponse<PageResult<ContentAuditRecord>> records(
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return InterviewResponse.ok(reviewService.records(auditStatus, riskLevel, keyword, page, pageSize));
    }

    @Operation(summary = "审核统计")
    @GetMapping("/stats")
    public InterviewResponse<Map<String, Object>> stats() {
        return InterviewResponse.ok(reviewService.stats());
    }

    // ==================== 请求体 DTO ====================
    //  说明：@RequestBody 直接用 Map<String, List<T>> 嵌套泛型时，
    //  反序列化会丢失内层元素类型（得到 List<LinkedHashMap>）导致 ClassCastException，
    //  必须用具体包装类承载泛型字段

    @lombok.Data
    public static class BatchWordsBody {
        private List<SensitiveWord> words;
    }

    @lombok.Data
    public static class DeleteWordsBody {
        private List<Long> ids;
    }
}
