package com.algoviz.controller;

import com.algoviz.integration.es.VectorSearchClient;
import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.vector.VectorCollectionInfo;
import com.algoviz.dto.vector.VectorPageResult;
import com.algoviz.dto.vector.VectorStats;
import com.algoviz.service.SyncProgressHolder;
import com.algoviz.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 向量数据库管理接口（管理端）
 * 路径前缀：/api/vector/admin
 */
@Tag(name = "向量数据库管理", description = "向量库同步、统计、清空、实时检测")
@RestController
@RequestMapping("/api/vector/admin")
@RequiredArgsConstructor
public class VectorAdminController {

    private final VectorSearchService vectorService;
    private final VectorSearchClient vectorClient;
    private final SyncProgressHolder progressHolder;

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public InterviewResponse<Map<String, Object>> health() {
        return InterviewResponse.ok(vectorService.health());
    }

    @Operation(summary = "向量库统计")
    @GetMapping("/stats")
    public InterviewResponse<VectorStats> stats() {
        return InterviewResponse.ok(vectorService.stats());
    }

    @Operation(summary = "同步任务进度", description = "获取向量同步/ES 同步的当前进度，前端 1s 轮询一次")
    @GetMapping("/sync/progress")
    public InterviewResponse<SyncProgressHolder.ProgressSnapshot> syncProgress() {
        return InterviewResponse.ok(progressHolder.snapshot());
    }

    @Operation(summary = "全量同步面试题到向量库")
    @PostMapping("/sync-all")
    public InterviewResponse<Map<String, Object>> syncAll() {
        return InterviewResponse.ok("同步完成", vectorService.syncAll());
    }

    @Operation(summary = "清空向量库")
    @PostMapping("/clear")
    public InterviewResponse<Map<String, Object>> clear() {
        return InterviewResponse.ok("向量库已清空", vectorService.clear());
    }

    // ===== ChromaDB 实时检测 =====

    @Operation(summary = "ChromaDB Collection 信息", description = "向量数、维度、距离度量、模型名等")
    @GetMapping("/collection-info")
    public InterviewResponse<VectorCollectionInfo> collectionInfo() {
        return InterviewResponse.ok(vectorClient.collectionInfo());
    }

    @Operation(summary = "ChromaDB 向量分页列表", description = "查看向量库中实际存储的向量及其对应题目信息")
    @GetMapping("/vectors")
    public InterviewResponse<VectorPageResult> vectors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        return InterviewResponse.ok(vectorClient.vectorsList(page, pageSize, keyword));
    }

    // ===== Elasticsearch 索引管理 =====

    @Operation(summary = "ES 健康检查")
    @GetMapping("/es/health")
    public InterviewResponse<Map<String, Object>> esHealth() {
        return InterviewResponse.ok(vectorService.esHealth());
    }

    @Operation(summary = "ES 索引统计", description = "索引是否存在、文档数、分词器类型")
    @GetMapping("/es/stats")
    public InterviewResponse<Map<String, Object>> esStats() {
        return InterviewResponse.ok(vectorService.esStats());
    }

    @Operation(summary = "全量同步面试题到 ES 索引", description = "把 MySQL 所有题目写入 ES（IK 分词索引）")
    @PostMapping("/es/sync-all")
    public InterviewResponse<Map<String, Object>> esSyncAll() {
        return InterviewResponse.ok("同步完成", vectorService.esSyncAll());
    }

    @Operation(summary = "重建 ES 索引", description = "删除并重建索引（切换分词器或 mapping 变更时使用）")
    @PostMapping("/es/recreate")
    public InterviewResponse<Map<String, Object>> esRecreate() {
        return InterviewResponse.ok(vectorService.esRecreateIndex());
    }

    @Operation(summary = "删除整个 ES 索引")
    @DeleteMapping("/es/index")
    public InterviewResponse<Map<String, Object>> esDeleteIndex() {
        return InterviewResponse.ok(vectorService.esDeleteIndexAll());
    }

    @Operation(summary = "测试 IK 分词器", description = "查看 IK 对输入文本的分词效果")
    @GetMapping("/es/test-analyzer")
    public InterviewResponse<Map<String, Object>> esTestAnalyzer(
            @RequestParam(defaultValue = "动态规划入门二叉树遍历") String text) {
        return InterviewResponse.ok(vectorService.esTestAnalyzer(text));
    }
}
