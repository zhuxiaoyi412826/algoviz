package com.algoviz.dto.vector;

import lombok.Data;
import java.util.List;

/**
 * 向量检索服务 — 搜索响应
 */
@Data
public class VectorSearchResponse {
    private List<VectorSearchResultItem> results;
    private int total;
    private String query;
}
