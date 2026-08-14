package com.algoviz.service;

import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.vector.*;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.mapper.InterviewProblemMapper;
import com.algoviz.client.VectorSearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量检索服务层
 * 1. 管理端：全量同步、单条同步、删除、统计、清空
 * 2. 用户端：语义搜索（向量召回 → MySQL 查完整题目）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorSearchClient vectorClient;
    private final InterviewProblemMapper problemMapper;

    // ==================== 管理端操作 ====================

    /**
     * 全量同步所有面试题到向量库
     */
    public Map<String, Object> syncAll() {
        List<InterviewProblem> all = problemMapper.listAllForExport(null, null);
        log.info("[向量同步] 开始同步 {} 条题目", all.size());

        List<VectorEmbedRequest> problems = all.stream().map(this::toEmbedRequest).toList();
        VectorBatchEmbedRequest batchReq = new VectorBatchEmbedRequest();
        batchReq.setProblems(problems);

        Map<String, Object> result = vectorClient.embedBatch(batchReq);
        log.info("[向量同步] 同步完成: {}", result);
        return result;
    }

    /**
     * 单条题目同步到向量库（增/改时触发）
     */
    public void syncSingle(InterviewProblem p) {
        try {
            VectorEmbedRequest req = toEmbedRequest(p);
            vectorClient.embedSingle(req);
            log.info("[向量同步] 单条同步成功: id={}", p.getId());
        } catch (Exception e) {
            log.warn("[向量同步] 单条同步失败: id={}, err={}", p.getId(), e.getMessage());
        }
    }

    /**
     * 删除向量
     */
    public void delete(Long problemId) {
        try {
            vectorClient.deleteEmbedding(problemId);
            log.info("[向量同步] 删除向量: id={}", problemId);
        } catch (Exception e) {
            log.warn("[向量同步] 删除失败: id={}, err={}", problemId, e.getMessage());
        }
    }

    /**
     * 向量库统计
     */
    public VectorStats stats() {
        try {
            return vectorClient.stats();
        } catch (Exception e) {
            log.warn("[向量服务] 统计获取失败: {}", e.getMessage());
            VectorStats stats = new VectorStats();
            stats.setStatus("offline");
            stats.setVectorCount(0);
            return stats;
        }
    }

    /**
     * 健康检查
     */
    public Map<String, Object> health() {
        try {
            return vectorClient.health();
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", "offline");
            m.put("message", e.getMessage());
            return m;
        }
    }

    /**
     * 清空向量库
     */
    public Map<String, Object> clear() {
        return vectorClient.clear();
    }

    // ==================== 用户端语义搜索 ====================

    /**
     * 语义搜索：向量召回 → MySQL 查完整题目
     * 降级策略：向量服务不可用时回退到 MySQL LIKE 搜索
     */
    public InterviewResponse<List<InterviewProblem>> semanticSearch(String query, int topK) {
        try {
            // 1. 向量召回
            VectorSearchRequest searchReq = new VectorSearchRequest();
            searchReq.setQuery(query);
            searchReq.setTopK(topK);
            VectorSearchResponse searchResp = vectorClient.search(searchReq);

            if (searchResp == null || searchResp.getResults() == null || searchResp.getResults().isEmpty()) {
                // 向量召回为空，降级到 MySQL
                log.info("[语义搜索] 向量召回为空，降级到 MySQL: query={}", query);
                return fallbackSearch(query);
            }

            // 2. 提取 problemId 列表
            List<Long> ids = searchResp.getResults().stream()
                    .map(VectorSearchResultItem::getProblemId)
                    .filter(Objects::nonNull)
                    .toList();

            if (ids.isEmpty()) {
                return fallbackSearch(query);
            }

            // 3. MySQL 查完整题目
            List<InterviewProblem> problems = problemMapper.selectByIds(ids);
            if (problems.isEmpty()) {
                return fallbackSearch(query);
            }

            // 4. 按相似度排序（向量结果顺序 = 相似度降序）
            Map<Long, InterviewProblem> idMap = problems.stream()
                    .collect(Collectors.toMap(InterviewProblem::getId, p -> p, (a, b) -> a));
            List<InterviewProblem> ordered = new ArrayList<>();
            for (VectorSearchResultItem r : searchResp.getResults()) {
                InterviewProblem p = idMap.get(r.getProblemId());
                if (p != null) {
                    ordered.add(p);
                }
            }

            return InterviewResponse.ok("语义搜索成功", ordered);
        } catch (Exception e) {
            log.warn("[语义搜索] 向量服务异常，降级到 MySQL: {}", e.getMessage());
            return fallbackSearch(query);
        }
    }

    /**
     * 降级搜索：MySQL LIKE
     */
    private InterviewResponse<List<InterviewProblem>> fallbackSearch(String query) {
        List<InterviewProblem> results = problemMapper.selectFrontSearch(query, 0, 20);
        return InterviewResponse.ok("关键词搜索（向量服务降级）", results);
    }

    // ==================== 工具方法 ====================

    /**
     * InterviewProblem → VectorEmbedRequest
     */
    private VectorEmbedRequest toEmbedRequest(InterviewProblem p) {
        VectorEmbedRequest req = new VectorEmbedRequest();
        req.setProblemId(p.getId());
        req.setProblemNo(p.getProblemNo());
        req.setTitle(p.getTitle());
        req.setTags(p.getTags());
        req.setCategory(p.getCategory());
        req.setDifficulty(p.getDifficulty());
        req.setDescription(p.getDescription());
        return req;
    }
}
