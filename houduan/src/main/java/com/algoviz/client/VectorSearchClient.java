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

    // ===== Elasticsearch 分词搜索 + 索引管理 =====

    /** ES 健康检查 */
    @GetMapping("/api/v1/es/health")
    Map<String, Object> esHealth();

    /** ES 分词搜索（IK 分词器） */
    @PostMapping("/api/v1/es-search")
    Map<String, Object> esSearch(@RequestBody ESSearchRequest request);

    /** ES 索引统计 */
    @GetMapping("/api/v1/es/stats")
    Map<String, Object> esStats();

    /** 单条题目写入 ES 索引 */
    @PostMapping("/api/v1/es/index/single")
    Map<String, Object> esIndexSingle(@RequestBody ESIndexSingleRequest request);

    /** 批量题目写入 ES 索引 */
    @PostMapping("/api/v1/es/index/batch")
    Map<String, Object> esIndexBatch(@RequestBody Map<String, Object> request);

    /** 从 ES 索引删除单条题目 */
    @DeleteMapping("/api/v1/es/index/{problemId}")
    Map<String, Object> esDeleteIndex(@PathVariable("problemId") Long problemId);

    /** 重建 ES 索引 */
    @PostMapping("/api/v1/es/index/recreate")
    Map<String, Object> esRecreateIndex();

    /** 删除整个 ES 索引 */
    @DeleteMapping("/api/v1/es/index")
    Map<String, Object> esDeleteIndexAll();

    /** 测试 IK 分词器 */
    @GetMapping("/api/v1/es/test-analyzer")
    Map<String, Object> esTestAnalyzer(@RequestParam(value = "text", defaultValue = "动态规划入门二叉树遍历") String text);
}

