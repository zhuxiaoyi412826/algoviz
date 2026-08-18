package com.algoviz.dto.vector;

import lombok.Data;

/**
 * ES 分词搜索请求（对应 Python /api/v1/es-search）
 */
@Data
public class ESSearchRequest {
    private String query;
    private int topK = 20;
    private String difficulty = "";
}
