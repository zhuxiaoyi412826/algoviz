package com.algoviz.service;

import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.vector.*;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.mapper.InterviewProblemMapper;
import com.algoviz.client.VectorSearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final SyncProgressHolder progress;

    /** 向量同步：每次调用 Python 的批量大小 */
    private static final int VECTOR_BATCH_SIZE = 100;
    /** ES 索引同步：每次调用 Python 的批量大小 */
    private static final int ES_BATCH_SIZE = 200;
    /** ES 日期格式：yyyy-MM-dd HH:mm:ss（与 ES mapping 一致） */
    private static final DateTimeFormatter ES_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 格式化 LocalDateTime 为 ES 可接受的字符串；null 时返回 null */
    private static String formatEsDate(LocalDateTime dt) {
        return dt == null ? null : dt.format(ES_DATE_FMT);
    }

    // ==================== 管理端操作 ====================

    /**
     * 全量同步所有面试题到向量库（异步执行，立即返回任务已提交）
     * 实际进度通过 SyncProgressHolder 暴露给 Vue 轮询。
     */
    public Map<String, Object> syncAll() {
        if (!progress.isRunning()) {
            // 预占：只有当前没任务时 compareAndSet(false, true)
            progress.startTask("vector_sync", 0);
        } else {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("submitted", false);
            m.put("message", "已有同步任务在执行中，请等待完成后再试");
            return m;
        }
        doAsyncVectorSyncAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("submitted", true);
        m.put("message", "已提交全量向量同步任务，请查看进度条");
        return m;
    }

    /**
     * 实际的向量全量同步异步任务
     * 在专用线程池执行，每批 100 条调用 Python，进度实时写入 progress
     */
    @Async("syncTaskExecutor")
    public void doAsyncVectorSyncAll() {
        long t0 = System.currentTimeMillis();
        try {
            progress.updateMessage("正在从 MySQL 读取面试题...");
            List<InterviewProblem> all = problemMapper.listAllForExport(null, null);
            if (all.isEmpty()) {
                progress.enterRunningPhase(0);
                progress.complete("数据库中暂无面试题");
                return;
            }

            progress.enterRunningPhase(all.size());
            log.info("[向量同步] 开始向量化 {} 条题目", all.size());

            // 2. 分批提交（每批 VECTOR_BATCH_SIZE 条）
            int success = 0;
            int fail = 0;
            int totalSize = all.size();

            for (int i = 0; i < totalSize; i += VECTOR_BATCH_SIZE) {
                int end = Math.min(i + VECTOR_BATCH_SIZE, totalSize);
                List<InterviewProblem> batch = all.subList(i, end);

                try {
                    List<VectorEmbedRequest> problems = batch.stream()
                            .map(this::toEmbedRequest)
                            .toList();
                    VectorBatchEmbedRequest batchReq = new VectorBatchEmbedRequest();
                    batchReq.setProblems(problems);

                    Map<String, Object> result = vectorClient.embedBatch(batchReq);

                    Object succObj = result.get("success");
                    Object failObj = result.get("failed");
                    int batchOk = (succObj instanceof Number n) ? n.intValue() : problems.size();
                    int batchFail = (failObj instanceof Number n) ? n.intValue() : 0;

                    success += batchOk;
                    fail += batchFail;
                    progress.addProcessed(batchOk);
                    progress.addFailed(batchFail);
                    progress.updateMessage(String.format(
                            "向量同步: 已处理 %d / %d (成功 %d, 失败 %d)",
                            success + fail, totalSize, success, fail
                    ));
                } catch (Exception e) {
                    int batchSize = batch.size();
                    fail += batchSize;
                    progress.addFailed(batchSize);
                    log.warn("[向量同步] 批次 {}-{} 异常: {}", i + 1, end, e.getMessage());
                    progress.updateMessage(String.format(
                            "向量同步: 已处理 %d / %d, 当前批次失败, 错误=%s",
                            success + fail, totalSize, e.getMessage()
                    ));
                }
            }

            long cost = System.currentTimeMillis() - t0;
            String msg = String.format(
                    "向量同步完成! 成功 %d, 失败 %d, 共 %d, 耗时 %.1f秒",
                    success, fail, totalSize, cost / 1000.0
            );
            log.info("[向量同步] {}", msg);
            progress.complete(msg);
        } catch (Exception e) {
            log.error("[向量同步] 异常终止", e);
            progress.fail(e.getMessage());
        }
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
     * 批量同步题目到向量库（批量导入题目时触发）
     * 如果此时没有正在进行的大任务，则启用进度上报；否则静默追加
     */
    public void syncBatch(List<InterviewProblem> problems) {
        if (problems == null || problems.isEmpty()) return;
        boolean useProgress = !progress.isRunning();
        int batchCount = problems.size();
        if (useProgress) {
            progress.startTask("vector_sync", batchCount);
            progress.enterRunningPhase(batchCount);
        }
        try {
            List<VectorEmbedRequest> reqList = problems.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getId() != null)
                    .map(this::toEmbedRequest)
                    .toList();
            if (reqList.isEmpty()) {
                if (useProgress) progress.complete("没有需要同步的题目");
                return;
            }
            VectorBatchEmbedRequest batchReq = new VectorBatchEmbedRequest();
            batchReq.setProblems(reqList);
            vectorClient.embedBatch(batchReq);
            if (useProgress) {
                progress.addProcessed(reqList.size());
                progress.complete(String.format("批量向量同步完成, 共 %d 条", reqList.size()));
            }
            log.info("[向量同步] 批量同步已提交: {} 条", reqList.size());
        } catch (Exception e) {
            if (useProgress) progress.fail(e.getMessage());
            log.warn("[向量同步] 批量同步失败: {} 条, err={}", problems.size(), e.getMessage());
        }
    }

    /**
     * 批量删除向量
     */
    public void deleteBatch(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) return;
        for (Long id : problemIds) {
            if (id != null) {
                delete(id);
            }
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
        req.setSolution(p.getSolution());
        return req;
    }

    // ==================== ES 索引管理 ====================

    /**
     * 单条题目同步到 ES 索引（增/改时触发）
     */
    public void esSyncSingle(InterviewProblem p) {
        try {
            ESIndexSingleRequest req = new ESIndexSingleRequest();
            req.setId(p.getId());
            req.setProblemNo(p.getProblemNo());
            req.setTitle(p.getTitle());
            req.setTags(p.getTags());
            req.setCategory(p.getCategory());
            req.setDifficulty(p.getDifficulty());
            req.setDescription(p.getDescription());
            // ES 索引的 content 字段：组合 title + tags + category + description 提高搜索召回
            req.setContent(p.getTitle() + " " + p.getTags() + " " + p.getCategory() + " " + p.getDescription());
            req.setViewCount(p.getViewCount() != null ? p.getViewCount() : 0);
            req.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            vectorClient.esIndexSingle(req);
            log.info("[ES 同步] 单条索引成功: id={}", p.getId());
        } catch (Exception e) {
            log.warn("[ES 同步] 单条索引失败: id={}, err={}", p.getId(), e.getMessage());
        }
    }

    /**
     * 批量同步题目到 ES 索引（批量导入时触发）
     * 如果此时没有正在进行的大任务，则启用进度上报
     */
    public void esSyncBatch(List<InterviewProblem> problems) {
        if (problems == null || problems.isEmpty()) return;
        boolean useProgress = !progress.isRunning();
        int batchCount = problems.size();
        if (useProgress) {
            progress.startTask("es_sync", batchCount);
            progress.enterRunningPhase(batchCount);
        }
        try {
            List<Map<String, Object>> problemList = new ArrayList<>();
            for (InterviewProblem p : problems) {
                if (p == null || p.getId() == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("problemNo", p.getProblemNo());
                item.put("title", p.getTitle());
                item.put("tags", p.getTags());
                item.put("category", p.getCategory());
                item.put("difficulty", p.getDifficulty());
                item.put("description", p.getDescription());
                item.put("content", p.getTitle() + " " + p.getTags() + " " + p.getCategory() + " " + p.getDescription());
                item.put("viewCount", p.getViewCount() != null ? p.getViewCount() : 0);
                item.put("createdAt", formatEsDate(p.getCreatedAt()));
                problemList.add(item);
            }
            if (problemList.isEmpty()) {
                if (useProgress) progress.complete("没有需要同步的题目");
                return;
            }
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("problems", problemList);
            vectorClient.esIndexBatch(request);
            if (useProgress) {
                progress.addProcessed(problemList.size());
                progress.complete(String.format("批量 ES 索引完成, 共 %d 条", problemList.size()));
            }
            log.info("[ES 同步] 批量索引已提交: {} 条", problemList.size());
        } catch (Exception e) {
            if (useProgress) progress.fail(e.getMessage());
            log.warn("[ES 同步] 批量索引失败: {} 条, err={}", problems.size(), e.getMessage());
        }
    }

    /**
     * 从 ES 索引删除单条题目
     */
    public void esDelete(Long problemId) {
        try {
            vectorClient.esDeleteIndex(problemId);
            log.info("[ES 同步] 删除索引: id={}", problemId);
        } catch (Exception e) {
            log.warn("[ES 同步] 删除失败: id={}, err={}", problemId, e.getMessage());
        }
    }

    /**
     * 批量从 ES 索引删除
     */
    public void esDeleteBatch(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) return;
        for (Long id : problemIds) {
            if (id != null) {
                esDelete(id);
            }
        }
    }

    /**
     * ES 分词搜索（前台用户使用）
     */
    public InterviewResponse<List<InterviewProblem>> esSearch(String query, int topK, String difficulty) {
        try {
            ESSearchRequest req = new ESSearchRequest();
            req.setQuery(query);
            req.setTopK(topK);
            req.setDifficulty(difficulty != null ? difficulty : "");
            Map<String, Object> result = vectorClient.esSearch(req);

            // 提取 problemId 列表
            Object resultsObj = result.get("results");
            if (!(resultsObj instanceof List<?> resultsList) || resultsList.isEmpty()) {
                return InterviewResponse.ok("ES 搜索无结果", List.of());
            }

            List<Long> ids = new ArrayList<>();
            for (Object item : resultsList) {
                if (item instanceof Map<?, ?> m) {
                    Object pid = m.get("problemId");
                    if (pid instanceof Number n) {
                        ids.add(n.longValue());
                    } else if (pid instanceof String s && !s.isEmpty()) {
                        try { ids.add(Long.parseLong(s)); } catch (Exception ignored) {}
                    }
                }
            }

            if (ids.isEmpty()) {
                return InterviewResponse.ok("ES 搜索无结果", List.of());
            }

            // 从 MySQL 查完整题目
            List<InterviewProblem> problems = problemMapper.selectByIds(ids);
            if (problems.isEmpty()) {
                return InterviewResponse.ok("ES 搜索结果题目不存在", List.of());
            }

            // 按 ES 返回顺序排序
            Map<Long, InterviewProblem> idMap = problems.stream()
                    .collect(Collectors.toMap(InterviewProblem::getId, p -> p, (a, b) -> a));
            List<InterviewProblem> ordered = new ArrayList<>();
            for (Long id : ids) {
                InterviewProblem p = idMap.get(id);
                if (p != null) {
                    ordered.add(p);
                }
            }

            return InterviewResponse.ok("ES 分词搜索成功", ordered);
        } catch (Exception e) {
            log.warn("[ES 搜索] 服务异常，降级到 MySQL: {}", e.getMessage());
            return fallbackSearch(query);
        }
    }

    /**
     * ES 索引统计
     */
    public Map<String, Object> esStats() {
        try {
            return vectorClient.esStats();
        } catch (Exception e) {
            log.warn("[ES 统计] 获取失败: {}", e.getMessage());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("exists", false);
            m.put("count", 0);
            m.put("message", e.getMessage());
            return m;
        }
    }

    /**
     * ES 健康检查
     */
    public Map<String, Object> esHealth() {
        try {
            return vectorClient.esHealth();
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", "offline");
            m.put("message", e.getMessage());
            return m;
        }
    }

    /**
     * 重建 ES 索引
     */
    public Map<String, Object> esRecreateIndex() {
        return vectorClient.esRecreateIndex();
    }

    /**
     * 全量同步所有面试题到 ES 索引（异步执行，立即返回已提交）
     */
    public Map<String, Object> esSyncAll() {
        if (!progress.isRunning()) {
            progress.startTask("es_sync", 0);
        } else {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("submitted", false);
            m.put("message", "已有同步任务在执行中，请等待完成后再试");
            return m;
        }
        doAsyncEsSyncAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("submitted", true);
        m.put("message", "已提交 ES 索引同步任务，请查看进度条");
        return m;
    }

    /**
     * ES 索引全量同步异步任务
     */
    @Async("syncTaskExecutor")
    public void doAsyncEsSyncAll() {
        long t0 = System.currentTimeMillis();
        try {
            progress.updateMessage("正在从 MySQL 读取面试题...");
            List<InterviewProblem> all = problemMapper.listAllForExport(null, null);
            if (all.isEmpty()) {
                progress.enterRunningPhase(0);
                progress.complete("数据库中暂无面试题");
                return;
            }

            progress.enterRunningPhase(all.size());
            log.info("[ES 同步] 开始同步 {} 条题目到 ES 索引", all.size());

            int success = 0;
            int fail = 0;
            int totalSize = all.size();

            for (int i = 0; i < totalSize; i += ES_BATCH_SIZE) {
                int end = Math.min(i + ES_BATCH_SIZE, totalSize);
                List<InterviewProblem> batch = all.subList(i, end);

                try {
                    List<Map<String, Object>> problemList = new ArrayList<>();
                    for (InterviewProblem p : batch) {
                        if (p == null || p.getId() == null) continue;
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", p.getId());
                        item.put("problemNo", p.getProblemNo());
                        item.put("title", p.getTitle());
                        item.put("tags", p.getTags());
                        item.put("category", p.getCategory());
                        item.put("difficulty", p.getDifficulty());
                        item.put("description", p.getDescription());
                        item.put("content", p.getTitle() + " " + p.getTags() + " " + p.getCategory() + " " + p.getDescription());
                        item.put("viewCount", p.getViewCount() != null ? p.getViewCount() : 0);
                        item.put("createdAt", formatEsDate(p.getCreatedAt()));
                        problemList.add(item);
                    }
                    if (problemList.isEmpty()) continue;

                    Map<String, Object> request = new LinkedHashMap<>();
                    request.put("problems", problemList);
                    Map<String, Object> result = vectorClient.esIndexBatch(request);

                    Object succObj = result.get("success");
                    Object failObj = result.get("failed");
                    int batchOk = (succObj instanceof Number n) ? n.intValue() : problemList.size();
                    int batchFail = (failObj instanceof Number n) ? n.intValue() : 0;

                    success += batchOk;
                    fail += batchFail;
                    progress.addProcessed(batchOk);
                    progress.addFailed(batchFail);
                    progress.updateMessage(String.format(
                            "ES 同步: 已处理 %d / %d (成功 %d, 失败 %d)",
                            success + fail, totalSize, success, fail
                    ));
                } catch (Exception e) {
                    int batchSize = batch.size();
                    fail += batchSize;
                    progress.addFailed(batchSize);
                    log.warn("[ES 同步] 批次 {}-{} 异常: {}", i + 1, end, e.getMessage());
                    progress.updateMessage(String.format(
                            "ES 同步: 已处理 %d / %d, 当前批次失败, 错误=%s",
                            success + fail, totalSize, e.getMessage()
                    ));
                }
            }

            long cost = System.currentTimeMillis() - t0;
            String msg = String.format(
                    "ES 索引同步完成! 成功 %d, 失败 %d, 共 %d, 耗时 %.1f秒",
                    success, fail, totalSize, cost / 1000.0
            );
            log.info("[ES 同步] {}", msg);
            progress.complete(msg);
        } catch (Exception e) {
            log.error("[ES 同步] 异常终止", e);
            progress.fail(e.getMessage());
        }
    }

    /**
     * 测试 IK 分词器
     */
    public Map<String, Object> esTestAnalyzer(String text) {
        return vectorClient.esTestAnalyzer(text);
    }

    /**
     * 删除整个 ES 索引
     */
    public Map<String, Object> esDeleteIndexAll() {
        return vectorClient.esDeleteIndexAll();
    }
}
