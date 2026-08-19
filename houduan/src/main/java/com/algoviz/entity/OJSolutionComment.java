package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** OJ 题解评论 */
@Data
@Schema(description = "OJ题解评论")
public class OJSolutionComment {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "题解ID")
    private Long solutionId;
    @Schema(description = "题目ID冗余")
    private Long problemId;
    @Schema(description = "评论用户ID")
    private Long userId;
    @Schema(description = "用户名冗余")
    private String username;
    @Schema(description = "头像冗余")
    private String avatar;

    @Schema(description = "父评论ID，0=顶层")
    private Long parentId;
    @Schema(description = "顶层评论ID")
    private Long rootId;
    @Schema(description = "回复目标用户ID")
    private Long replyToUserId;
    @Schema(description = "回复目标用户名")
    private String replyToUsername;

    @Schema(description = "评论正文")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "审核状态")
    private String auditStatus;
    @Schema(description = "风险等级")
    private String riskLevel;
    @Schema(description = "检测命中摘要")
    private String detectSummary;

    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private String createdAt;
    @Schema(description = "更新时间")
    private String updatedAt;

    /** 屏蔽版字段 */
    @Schema(description = "评论正文（屏蔽版）")
    private transient String maskContent;
    @Schema(description = "当前用户是否已点赞")
    private transient Boolean liked;
    @Schema(description = "子评论数")
    private transient Integer replyCount;
}
