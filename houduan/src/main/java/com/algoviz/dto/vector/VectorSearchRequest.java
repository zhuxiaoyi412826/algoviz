package com.algoviz.dto.vector;

import lombok.Data;

/**
 * 向量检索服务 — 语义搜索请求
 */
@Data
public class VectorSearchRequest {
    private String query;
    private int topK = 10;
    private double threshold = 0.35;
}
