package com.algoviz.controller;

import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量语义搜索接口（用户端）
 * 路径前缀：/api/vector
 */
@Tag(name = "向量语义搜索", description = "用户端语义搜索面试题")
@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
public class VectorSearchController {

    private final VectorSearchService vectorService;

    @Operation(summary = "语义搜索面试题", description = "通过向量检索搜索题目，向量服务不可用时自动降级到关键词搜索")
    @GetMapping("/search")
    public InterviewResponse<List<InterviewProblem>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {
        if (query == null || query.trim().isEmpty()) {
            return InterviewResponse.fail(400, "搜索内容不能为空");
        }
        return vectorService.semanticSearch(query.trim(), topK);
    }
}
