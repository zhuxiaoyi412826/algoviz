package com.algoviz.dto.vector;

import lombok.Data;

/**
 * 向量检索服务 — 统计信息
 */
@Data
public class VectorStats {
    private String collectionName;
    private int vectorCount;
    private String modelName;
    private String status;
}
