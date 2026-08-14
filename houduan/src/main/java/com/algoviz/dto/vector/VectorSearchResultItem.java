package com.algoviz.dto.vector;

import lombok.Data;

/**
 * 向量检索服务 — 单条检索结果
 */
@Data
public class VectorSearchResultItem {
    private Long problemId;
    private String problemNo;
    private String title;
    private double similarity;
    private String category;
    private String difficulty;
    private String tags;
}
