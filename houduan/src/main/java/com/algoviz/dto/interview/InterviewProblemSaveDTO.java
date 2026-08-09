package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 后台保存面试题请求体（新增/修改） */
@Data
@Schema(description = "面试题保存请求体")
public class InterviewProblemSaveDTO {
    @Schema(description = "题目编号，为空时系统自动分配")
    private String problemNo;
    @Schema(description = "标题（必填）")
    private String title;
    @Schema(description = "难度：easy/medium/hard（必填）")
    private String difficulty;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "标签（逗号分隔或数组）")
    private Object tags;
    @Schema(description = "题目描述 Markdown")
    private String description;
    @Schema(description = "输入格式 Markdown")
    private String inputFormat;
    @Schema(description = "输出格式 Markdown")
    private String outputFormat;
    @Schema(description = "题解 Markdown（必填）")
    private String solution;
    @Schema(description = "状态：ACTIVE/INACTIVE")
    private String status;
    @Schema(description = "是否高频：1/0")
    private Integer isFrequent;
}
