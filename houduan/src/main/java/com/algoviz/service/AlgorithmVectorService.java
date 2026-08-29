package com.algoviz.service;

import com.algoviz.entity.OJProblem;
import com.algoviz.know.api.KnowSearchService;
import com.algoviz.know.api.dto.*;
import com.algoviz.mapper.OJProblemMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 算法题目向量检索服务（门面）：
 * - 通过 Dubbo 直连独立服务 know-qrdant（check=false + mock 降级，未启动不影响主服务）
 * - 全量同步：后台页面"手动"触发，从 algorithm 表读取 → 向量化 → 写入 Qdrant（异步 + 进度）
 */
@Service
public class AlgorithmVectorService {

    @DubboReference(check = false, timeout = 3000, retries = 0, mock = "true",
            url = "${know.qrdant.url:dubbo://127.0.0.1:20880}")
    private KnowSearchService knowSearchService;

    private final OJProblemMapper ojProblemMapper;

    public AlgorithmVectorService(OJProblemMapper ojProblemMapper) {
        this.ojProblemMapper = ojProblemMapper;
    }

    // ==================== 进度 ====================

    public static class SyncProgress {
        public volatile boolean inProgress = false;
        public volatile String phase = "idle";       // idle/preparing/running/completed/failed/cancelled
        public volatile int processed = 0;
        public volatile int failed = 0;
        public volatile int total = 0;
        public volatile String message = "空闲中";
        public volatile String lastError = null;
        public volatile long startAtMs = 0;
    }

    private final SyncProgress progress = new SyncProgress();
    private final AtomicBoolean syncLock = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    /** 请求取消正在进行的同步；未在同步时不做任何事（避免污染残留状态） */
    public boolean cancelSync() {
        if (!progress.inProgress) {
            return false;
        }
        cancelRequested.set(true);
        progress.message = "正在取消...";
        return true;
    }

    public Map<String, Object> progressSnapshot() {
        long now = System.currentTimeMillis();
        long elapsedMs = progress.startAtMs > 0 ? (now - progress.startAtMs) : 0;
        long elapsedSeconds = elapsedMs / 1000;
        Long estimatedSeconds = null;
        // 仅在稳定运行一段时间后估算，避免刚开始（时间极短）外推出离谱值；
        // 失败/取消/暂停时不估算（前端显示"计算中"）
        if (progress.inProgress && "running".equals(progress.phase)
                && progress.processed > 0 && progress.total > 0 && elapsedMs >= 3000) {
            long speed = progress.processed * 1000L / elapsedMs;      // 条/秒
            long remain = Math.max(0, progress.total - progress.processed);
            if (speed > 0) {
                estimatedSeconds = remain * 1000L / speed;
                estimatedSeconds = Math.min(estimatedSeconds, 3600L); // 最多显示 1 小时
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inProgress", progress.inProgress);
        m.put("phase", progress.phase);
        m.put("processed", progress.processed);
        m.put("failed", progress.failed);
        m.put("total", progress.total);
        m.put("remaining", Math.max(0, progress.total - progress.processed));
        int pct = progress.total > 0 ? (int) (progress.processed * 100.0 / progress.total) : 0;
        m.put("percent", Math.min(100, pct));
        m.put("message", progress.message);
        m.put("lastError", progress.lastError);
        m.put("elapsedSeconds", elapsedSeconds);
        m.put("estimatedRemainingSeconds", estimatedSeconds);
        return m;
    }

    // ==================== 可用性 / 探活 ====================

    /** 探活：任何异常（连不上/decode 失败/业务异常）都降级为不可用，绝不让调用方 500 */
    public KnowServiceStatus ping() {
        try {
            return knowSearchService.ping();
        } catch (Exception e) {
            KnowServiceStatus s = new KnowServiceStatus();
            s.setModelReady(false);
            s.setQdrantConnected(false);
            s.setDim(1024);
            s.setCollectionName("algorithm_knowledge");
            s.setTotal(0);
            s.setMessage("向量检索服务未启动: " + e.getMessage());
            return s;
        }
    }

    public boolean isAvailable() {
        KnowServiceStatus s = ping();
        return s.isModelReady() && s.isQdrantConnected();
    }

    /** 操作类调用统一兜底：任何异常 → 抛"服务未启动"，由 Controller 转 404 */
    private <T> T guard(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new IllegalStateException("向量检索服务未启动", e);
        }
    }

    // ==================== 手动全量同步（算法题 → 向量库） ====================

    /** 启动全量同步；已有任务进行中返回 false */
    public boolean startSync() {
        if (!syncLock.compareAndSet(false, true)) {
            return false;
        }
        cancelRequested.set(false);
        progress.inProgress = true;
        progress.phase = "running";
        progress.processed = 0;
        progress.failed = 0;
        progress.total = 0;
        progress.message = "准备中...";
        progress.lastError = null;
        progress.startAtMs = System.currentTimeMillis();
        CompletableFuture.runAsync(this::doSync);
        return true;
    }

    private void doSync() {
        try {
            List<OJProblem> list = ojProblemMapper.getProblemsByStatus("ACTIVE");
            progress.total = list.size();
            progress.message = "开始向量化入库 " + list.size() + " 条算法题目";
            List<KnowUpsertItem> batch = new ArrayList<>();
            int done = 0;
            for (OJProblem p : list) {
                // 取消检查：用户点 X 后停止入库
                if (cancelRequested.get()) {
                    progress.phase = "cancelled";
                    progress.message = "已取消，已处理 " + done + " / " + list.size() + " 条";
                    break;
                }
                batch.add(new KnowUpsertItem(String.valueOf(p.getId()), buildText(p), buildPayload(p)));
                if (batch.size() >= 16) {
                    flushBatch(batch);
                }
                progress.processed = ++done;
            }
            flushBatch(batch);
            if (!"cancelled".equals(progress.phase)) {
                progress.phase = "completed";
                progress.message = "同步完成，成功 " + (progress.total - progress.failed) + " / 失败 " + progress.failed;
            }
        } catch (Exception e) {
            progress.phase = "failed";
            progress.lastError = e.getMessage();
            progress.message = "同步失败";
        } finally {
            progress.inProgress = false;
            cancelRequested.set(false);
            syncLock.set(false);
        }
    }

    private void flushBatch(List<KnowUpsertItem> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            int ok = knowSearchService.upsertBatch(new ArrayList<>(batch));
            progress.failed += Math.max(0, batch.size() - ok);
        } catch (Exception e) {
            progress.failed += batch.size();
            progress.lastError = "批量写入失败: " + e.getMessage();
        }
        batch.clear();
    }

    /** 组装向量化文本：只同步题目标题（内容太多，不同步全文；检索按标题近似匹配） */
    private String buildText(OJProblem p) {
        return p.getTitle() == null ? "" : p.getTitle().trim();
    }

    private Map<String, String> buildPayload(OJProblem p) {
        Map<String, String> pl = new LinkedHashMap<>();
        pl.put("algorithmId", String.valueOf(p.getId()));
        pl.put("problemNo", p.getProblemNo() == null ? "" : p.getProblemNo());
        pl.put("name", p.getTitle() == null ? "" : p.getTitle());
        pl.put("category", p.getDifficulty() == null ? "" : p.getDifficulty());
        if (p.getTags() != null && !p.getTags().isBlank()) {
            pl.put("tags", p.getTags());
        }
        // 实际送入模型的向量化文本（当前=标题；未来 buildText 扩展时自动跟随）
        pl.put("vectorizedText", buildText(p));
        return pl;
    }

    // ==================== 查询 / 管理 ====================

    public KnowStats stats() {
        return guard(knowSearchService::stats);
    }

    public KnowCollectionInfo collectionInfo() {
        return guard(knowSearchService::collectionInfo);
    }

    public KnowSearchResult search(String query, int topK, String category) {
        KnowSearchRequest req = new KnowSearchRequest();
        req.setQuery(query);
        req.setTopK(topK);
        req.setCategory(category);
        return guard(() -> knowSearchService.search(req));
    }

    public KnowPageResult list(int page, int pageSize, String keyword, String algorithmId, String category, String tags) {
        return guard(() -> knowSearchService.list(page, pageSize, keyword, algorithmId, category, tags));
    }

    public boolean delete(String algorithmId) {
        return guard(() -> knowSearchService.delete(algorithmId));
    }

    public long clear() {
        return guard(knowSearchService::clear);
    }
}
