package com.algoviz.client;

import com.algoviz.dto.vector.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OpenFeign 客户端 — 调用 Python 向量检索服务
 */
@FeignClient(name = "vector-service", url = "${vector.service.url}")
public interface VectorSearchClient {

    /** 健康检查 */
    @GetMapping("/health")
    Map<String, Object> health();

    /** 向量库统计 */
    @GetMapping("/api/v1/stats")
    VectorStats stats();

    /** 单条题目向量化入库 */
    @PostMapping("/api/v1/embedding/single")
    Map<String, Object> embedSingle(@RequestBody VectorEmbedRequest request);

    /** 批量向量化入库 */
    @PostMapping("/api/v1/embedding/batch")
    Map<String, Object> embedBatch(@RequestBody VectorBatchEmbedRequest request);

    /** 语义检索 */
    @PostMapping("/api/v1/search")
    VectorSearchResponse search(@RequestBody VectorSearchRequest request);

    /** 删除单条向量 */
    @DeleteMapping("/api/v1/embedding/{problemId}")
    Map<String, Object> deleteEmbedding(@PathVariable("problemId") Long problemId);

    /** 清空向量库 */
    @PostMapping("/api/v1/clear")
    Map<String, Object> clear();

    /** Collection 实时信息（向量数、维度、距离度量等） */
    @GetMapping("/api/v1/collection/info")
    VectorCollectionInfo collectionInfo();

    /** 向量分页列表（含对应题目 metadata） */
    @GetMapping("/api/v1/vectors")
    VectorPageResult vectorsList(@RequestParam("page") int page,
                                  @RequestParam("pageSize") int pageSize,
                                  @RequestParam(value = "keyword", defaultValue = "") String keyword);
}
