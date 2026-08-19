package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** OJ 用户题解 */
@Data
@Schema(description = "OJ用户题解")
public class OJSolution {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "题目ID")
    private Long problemId;
    @Schema(description = "题目标题冗余")
    private String problemTitle;
    @Schema(description = "发布用户ID")
    private Long userId;
    @Schema(description = "用户名冗余")
    private String username;
    @Schema(description = "头像冗余")
    private String avatar;

    @Schema(description = "题解标题")
    private String title;
    @Schema(description = "解题格式")
    private String format;
    @Schema(description = "思路")
    private String idea;
    @Schema(description = "解题过程")
    private String process;
    @Schema(description = "复杂度分析")
    private String complexity;
    @Schema(description = "代码语言")
    private String codeLang;
    @Schema(description = "题解代码")
    private String code;

    @Schema(description = "点赞数")
    private Integer likeCount;
    @Schema(description = "观看数")
    private Integer viewCount;
    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否已AC该题")
    private Integer isPassed;
    @Schema(description = "精选置顶")
    private Integer isFeatured;

    @Schema(description = "审核状态")
    private String auditStatus;
    @Schema(description = "风险等级")
    private String riskLevel;
    @Schema(description = "检测命中摘要")
    private String detectSummary;

    @Schema(description = "状态 PUBLISHED/HIDDEN/DELETED")
    private String status;
    @Schema(description = "创建时间")
    private String createdAt;
    @Schema(description = "更新时间")
    private String updatedAt;

    /** 屏蔽版字段（仅查询返回时填充，不入库） */
    @Schema(description = "思路（屏蔽版）")
    private transient String maskIdea;
    @Schema(description = "解题过程（屏蔽版）")
    private transient String maskProcess;
    @Schema(description = "代码（屏蔽版）")
    private transient String maskCode;
    @Schema(description = "当前用户是否已点赞")
    private transient Boolean liked;
}
