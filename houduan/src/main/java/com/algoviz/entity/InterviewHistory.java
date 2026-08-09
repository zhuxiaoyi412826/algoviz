package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "面试题目-浏览历史")
public class InterviewHistory {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "题目ID")
    private Long problemId;
    @Schema(description = "浏览时间")
    private LocalDateTime viewTime;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
