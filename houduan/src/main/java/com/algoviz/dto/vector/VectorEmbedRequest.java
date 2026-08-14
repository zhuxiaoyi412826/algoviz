package com.algoviz.dto.vector;

import lombok.Data;

/**
 * 向量检索服务 — 单条题目入库请求
 */
@Data
public class VectorEmbedRequest {
    private Long problemId;
    private String title;
    private String tags;
    private String category;
    private String difficulty;
    private String description;
    private String solution;
    private String problemNo;
}
