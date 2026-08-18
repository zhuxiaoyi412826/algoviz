package com.algoviz.dto.vector;

import lombok.Data;

/**
 * ES 索引单条写入请求（对应 Python /api/v1/es/index/single）
 */
@Data
public class ESIndexSingleRequest {
    private Long id;
    private String problemNo;
    private String title;
    private String tags;
    private String category;
    private String difficulty;
    private String description;
    private String content;
    private Integer viewCount;
    private String createdAt;
}
