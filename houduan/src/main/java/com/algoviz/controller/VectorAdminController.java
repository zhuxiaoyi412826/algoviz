package com.algoviz.controller;

import com.algoviz.client.VectorSearchClient;
import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.vector.VectorCollectionInfo;
import com.algoviz.dto.vector.VectorPageResult;
import com.algoviz.dto.vector.VectorStats;
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
}
