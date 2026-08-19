package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 点赞去重 */
@Data
@Schema(description = "题解/评论点赞")
public class OJSolutionLike {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "点赞用户ID")
    private Long userId;
    @Schema(description = "目标类型 SOLUTION/COMMENT")
    private String targetType;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "创建时间")
    private String createdAt;
}
