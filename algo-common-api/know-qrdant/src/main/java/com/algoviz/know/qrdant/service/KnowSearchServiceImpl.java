package com.algoviz.know.qrdant.service;

import com.algoviz.know.api.KnowSearchService;
import com.algoviz.know.api.dto.*;
import com.algoviz.know.qrdant.config.QdrantProperties;
import com.algoviz.know.qrdant.embedding.BgeEmbeddingService;
import com.algoviz.know.qrdant.qdrant.QdrantRestClient;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量检索 Dubbo Provider 实现：算法题目知识库（Qdrant + bge-large-zh-v1.5 1024 维）。
 * 数据由主服务后台页面"手动全量同步"触发写入（upsertBatch），本服务不主动拉取数据库。
 */
@DubboService
public class KnowSearchServiceImpl implements KnowSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowSearchServiceImpl.class);

    private final QdrantProperties qdrantProps;
    private final BgeEmbeddingService embeddingService;
    private final QdrantRestClient qdrant;

    public KnowSearchServiceImpl(QdrantProperties qdrantProps,
                                 BgeEmbeddingService embeddingService,
                                 QdrantRestClient qdrant) {
        this.qdrantProps = qdrantProps;
        this.embeddingService = embeddingService;
        this.qdrant = qdrant;
    }

    @Override
    public KnowServiceStatus ping() {
        KnowServiceStatus s = new KnowServiceStatus();
        s.setModelReady(embeddingService.isReady());
        s.setDim(qdrantProps.getDim());
        s.setCollectionName(qdrantProps.getCollection());
        try {
            qdrant.ensureCollection();
            s.setQdrantConnected(true);
            s.setTotal(qdrant.count());
        } catch (Exception e) {
            s.setQdrantConnected(false);
            s.setMessage("Qdrant 连接失败: " + e.getMessage());
        }
        if (s.isModelReady() && s.isQdrantConnected()) {
            s.setMessage("ok");
        }
        return s;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        return embeddingService.embed(text, false);
    }

    @Override
    public KnowUpsertResult upsert(KnowUpsertItem item) {
        if (item == null || item.getId() == null || item.getText() == null || item.getText().isBlank()) {
            return KnowUpsertResult.fail("id 或 text 为空");
        }
        try {
            float[] vec = embeddingService.embed(item.getText(), false);
            qdrant.ensureCollection();
            qdrant.upsert(List.of(new QdrantRestClient.PointInput(
                    QdrantRestClient.stableUuid(item.getId()), vec, item.getPayload())));
            return KnowUpsertResult.ok(item.getId());
        } catch (Exception e) {
            log.warn("upsert 失败 id={}: {}", item.getId(), e.getMessage());
            return KnowUpsertResult.fail(e.getMessage());
        }
    }

    @Override
    public int upsertBatch(List<KnowUpsertItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        qdrant.ensureCollection();
        List<QdrantRestClient.PointInput> batch = new ArrayList<>();
        int success = 0;
        for (KnowUpsertItem item : items) {
            try {
                if (item == null || item.getId() == null || item.getText() == null || item.getText().isBlank()) {
                    continue;
                }
                float[] vec = embeddingService.embed(item.getText(), false);
                batch.add(new QdrantRestClient.PointInput(
                        QdrantRestClient.stableUuid(item.getId()), vec, item.getPayload()));
                success++;
                if (batch.size() >= 64) {
                    qdrant.upsert(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("向量化失败 id={}: {}", item == null ? "?" : item.getId(), e.getMessage());
            }
        }
        if (!batch.isEmpty()) {
            qdrant.upsert(batch);
        }
        return success;
    }

    @Override
    public KnowSearchResult search(KnowSearchRequest request) {
        long t0 = System.currentTimeMillis();
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int topK = request.getTopK() <= 0 ? 10 : request.getTopK();
        float[] vec = embeddingService.embed(request.getQuery(), true); // query 加 BGE 前缀
        qdrant.ensureCollection();
        List<QdrantRestClient.Hit> hits = qdrant.search(vec, topK, request.getCategory());

        KnowSearchResult result = new KnowSearchResult();
        List<KnowSearchResultItem> items = new ArrayList<>();
        for (QdrantRestClient.Hit h : hits) {
            KnowSearchResultItem it = new KnowSearchResultItem();
            it.setId(String.valueOf(h.payload().getOrDefault("algorithmId", h.id())));
            it.setScore(h.score());
            it.setPayload(toStringMap(h.payload()));
            items.add(it);
        }
        result.setItems(items);
        result.setTotal(items.size());
        result.setTookMs(System.currentTimeMillis() - t0);
        return result;
    }

    @Override
    public boolean delete(String id) {
        try {
            qdrant.ensureCollection();
            return qdrant.deleteByAlgorithmId(id);
        } catch (Exception e) {
            log.warn("delete 失败 id={}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public long clear() {
        try {
            qdrant.ensureCollection();
            return qdrant.recreateCollection();
        } catch (Exception e) {
            log.warn("clear 失败: {}", e.getMessage());
            return -1;
        }
    }

    @Override
    public KnowStats stats() {
        KnowStats s = new KnowStats();
        s.setCollectionName(qdrantProps.getCollection());
        s.setDim(qdrantProps.getDim());
        s.setDistance(qdrantProps.getDistance());
        s.setTotal(0);
        s.setStatus("offline");
        try {
            qdrant.ensureCollection();
            s.setTotal(qdrant.count());
            s.setStatus("green");
        } catch (Exception e) {
            log.warn("stats: Qdrant 不可用，返回离线: {}", e.getMessage());
        }
        return s;
    }

    @Override
    public KnowCollectionInfo collectionInfo() {
        KnowCollectionInfo info = new KnowCollectionInfo();
        info.setCollectionName(qdrantProps.getCollection());
        info.setDim(qdrantProps.getDim());
        info.setDistance(qdrantProps.getDistance());
        try {
            qdrant.ensureCollection();
            Map<String, Object> cfg = new LinkedHashMap<>();
            qdrant.collectionInfo().fields().forEachRemaining(e -> cfg.put(e.getKey(), e.getValue().asText()));
            info.setConfig(cfg);
        } catch (Exception e) {
            log.warn("collectionInfo: Qdrant 不可用: {}", e.getMessage());
        }
        return info;
    }

    @Override
    public KnowPageResult list(int page, int pageSize, String keyword, String algorithmId, String category, String tags) {
        int p = Math.max(1, page);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        String kw = keyword == null ? "" : keyword.trim();
        String idF = algorithmId == null ? "" : algorithmId.trim();
        String catF = category == null ? "" : category.trim();
        String tagF = tags == null ? "" : tags.trim();
        KnowPageResult result = new KnowPageResult();
        result.setList(new ArrayList<>());
        result.setTotal(0);
        result.setPage(p);
        result.setPageSize(ps);
        try {
            qdrant.ensureCollection();
            // 滚动拉取（算法题量级小，直接过滤后内存分页，保证组合过滤稳定）
            List<Map<String, Object>> all = new ArrayList<>();
            Object offset = null;
            while (true) {
                List<Map<String, Object>> chunk = qdrant.scroll(200, offset);
                all.addAll(chunk);
                if (chunk.size() < 200) {
                    break;
                }
                Map<String, Object> last = chunk.get(chunk.size() - 1);
                offset = last.get("_id");
            }
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> m : all) {
                String id = String.valueOf(m.getOrDefault("algorithmId", ""));
                String name = String.valueOf(m.getOrDefault("name", ""));
                String cat = String.valueOf(m.getOrDefault("category", ""));
                String tag = String.valueOf(m.getOrDefault("tags", ""));
                if (!kw.isEmpty() && !name.contains(kw)) {
                    continue;
                }
                if (!idF.isEmpty() && !id.equals(idF) && !id.contains(idF)) {
                    continue;
                }
                if (!catF.isEmpty() && !cat.equals(catF)) {
                    continue;
                }
                if (!tagF.isEmpty() && !tag.contains(tagF)) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                m.forEach((k, v) -> {
                    if ("vector".equals(k)) {
                        row.put(k, v);          // 完整向量数组 List<Double>
                    } else {
                        row.put(k, v == null ? "" : String.valueOf(v));
                    }
                });
                filtered.add(row);
            }
            int total = filtered.size();
            int from = (p - 1) * ps;
            int to = Math.min(from + ps, total);
            result.setList(from >= total ? new ArrayList<>() : new ArrayList<>(filtered.subList(from, to)));
            result.setTotal(total);
        } catch (Exception e) {
            log.warn("list: Qdrant 不可用: {}", e.getMessage());
        }
        return result;
    }

    private static Map<String, String> toStringMap(Map<String, Object> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((k, v) -> out.put(k, v == null ? "" : String.valueOf(v)));
        }
        return out;
    }
}
