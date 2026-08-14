package com.algoviz.dto.vector;

import lombok.Data;
import java.util.List;

/**
 * 向量检索服务 — 批量入库请求
 */
@Data
public class VectorBatchEmbedRequest {
    private List<VectorEmbedRequest> problems;
}
