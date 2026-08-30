package com.algoviz.know.api;

import com.algoviz.know.api.dto.*;

import java.util.List;

/**
 * 向量检索 Dubbo 服务（算法题目知识库）。
 * 独立服务 know-qrdant 实现（Provider），主服务 houduan 引用（Consumer，直连 dubbo://ip:20880）。
 * 容错约定：主服务以 check=false + mock 降级方式调用 —— know-qrdant 未启动不影响主服务核心功能，
 * 仅算法题向量检索接口返回"服务未启动"。
 */
public interface KnowSearchService {

    /** 探活：返回服务状态（模型是否就绪 / Qdrant 是否连通 / 向量维度） */
    KnowServiceStatus ping();

    /** 文本 → 向量（bge-large-zh-v1.5，1024 维；内部自动加 BGE 检索前缀并归一化） */
    float[] embed(String text);

    /** 单条向量入库（先向量化再写 Qdrant） */
    KnowUpsertResult upsert(KnowUpsertItem item);

    /** 批量向量入库（重新向量化入库用），返回成功条数 */
    int upsertBatch(List<KnowUpsertItem> items);

    /** 语义检索（文本 → 向量 → Qdrant 余弦相似度 TopK） */
    KnowSearchResult search(KnowSearchRequest request);

    /** 按 id 删除向量 */
    boolean delete(String id);

    /** 清空集合全部向量，返回删除条数 */
    long clear();

    /** 集合统计（总数 / 维度 / 距离度量 / 状态） */
    KnowStats stats();

    /** 集合信息（用于管理页展示） */
    KnowCollectionInfo collectionInfo();

    /** 分页列出向量及 payload（支持组合过滤：keyword 标题模糊 / algorithmId 题目ID / category 难度 / tags 标签） */
    KnowPageResult list(int page, int pageSize, String keyword, String algorithmId, String category, String tags);
}
