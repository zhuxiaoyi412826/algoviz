package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 收藏列表 / 历史列表中每一项（含题目信息） */
@Data
@Schema(description = "前台收藏/历史列表项")
public class InterviewUserListVO {
    @Schema(description = "题目ID")
    private Long id;
    @Schema(description = "题目编号")
    private String problemNo;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "难度")
    private String difficulty;
    @Schema(description = "难度中文")
    private String difficultyLabel;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "阅读量")
    private Integer viewCount;
    @Schema(description = "是否高频")
    private Integer isFrequent;
    @Schema(description = "收藏时间（收藏列表时填充）")
    private String collectTime;
    @Schema(description = "浏览时间（历史列表时填充）")
    private String viewTime;
}
