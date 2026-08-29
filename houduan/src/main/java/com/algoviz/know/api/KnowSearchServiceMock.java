package com.algoviz.know.api;

import com.algoviz.know.api.dto.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Dubbo mock 降级实现（mock="true" 时由 Dubbo 自动调用）：
 * know-qrdant 独立服务未启动/不可用时，主服务核心功能不受影响，
 * 算法题向量相关调用返回"服务未启动"降级结果。
 */
public class KnowSearchServiceMock implements KnowSearchService {

    private static final String MSG = "向量检索服务未启动";

    @Override
    public KnowServiceStatus ping() {
        KnowServiceStatus s = new KnowServiceStatus();
        s.setModelReady(false);
        s.setQdrantConnected(false);
        s.setDim(1024);
        s.setCollectionName("algorithm_knowledge");
        s.setTotal(0);
        s.setMessage(MSG);
        return s;
    }

    @Override
    public float[] embed(String text) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public KnowUpsertResult upsert(KnowUpsertItem item) {
        return KnowUpsertResult.fail(MSG);
    }

    @Override
    public int upsertBatch(List<KnowUpsertItem> items) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public KnowSearchResult search(KnowSearchRequest request) {
        KnowSearchResult r = new KnowSearchResult();
        r.setItems(new ArrayList<>());
        r.setTotal(0);
        r.setTookMs(0);
        return r;
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public long clear() {
        return -1;
    }

    @Override
    public KnowStats stats() {
        KnowStats s = new KnowStats();
        s.setCollectionName("algorithm_knowledge");
        s.setDim(1024);
        s.setDistance("Cosine");
        s.setTotal(0);
        s.setStatus("offline");
        return s;
    }

    @Override
    public KnowCollectionInfo collectionInfo() {
        return new KnowCollectionInfo();
    }

    @Override
    public KnowPageResult list(int page, int pageSize, String keyword, String algorithmId, String category, String tags) {
        KnowPageResult r = new KnowPageResult();
        r.setList(new ArrayList<>());
        r.setTotal(0);
        r.setPage(page);
        r.setPageSize(pageSize);
        return r;
    }
}
