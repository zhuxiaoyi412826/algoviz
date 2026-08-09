package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "面试题目标签字典")
public class InterviewTag {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "标签名（唯一）")
    private String name;
    @Schema(description = "标签分类")
    private String category;
    @Schema(description = "使用次数")
    private Integer useCount;
    @Schema(description = "排序值，小在前")
    private Integer sortOrder;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
