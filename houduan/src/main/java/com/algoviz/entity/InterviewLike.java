package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "面试题目-点赞点踩")
public class InterviewLike {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "题目ID")
    private Long problemId;
    @Schema(description = "类型：like/dislike")
    private String type;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
