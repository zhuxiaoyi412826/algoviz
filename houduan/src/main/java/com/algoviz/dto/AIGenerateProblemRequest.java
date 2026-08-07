package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * AI 生成题目请求 DTO
 * 对应管理后台「AI 生成题目」配置表单
 */
@Schema(description = "AI生成题目请求")
public class AIGenerateProblemRequest {

    /** 知识点 / 标签（与现有 tags 字段兼容：数组,链表,树,图,动态规划,回溯,贪心,分治,排序,查找,字符串,数学 等） */
    @Schema(description = "知识点列表")
    private List<String> knowledgePoints;

    /** 难度：easy / medium / hard */
    @Schema(description = "难度")
    private String difficulty;

    /** 生成题目数量，1~10 */
    @Schema(description = "生成数量")
    private Integer count;

    /** 语言：java / python / cpp / javascript / general（通用） */
    @Schema(description = "编程语言")
    private String language;

    /** 题目风格：standard（常规题）/ variant（变式题）/ scenario（场景应用题） */
    @Schema(description = "题目风格")
    private String style;

    /** 用户额外补充要求，可空 */
    @Schema(description = "额外要求")
    private String additionalRequirements;

    public List<String> getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(List<String> knowledgePoints) { this.knowledgePoints = knowledgePoints; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getAdditionalRequirements() { return additionalRequirements; }
    public void setAdditionalRequirements(String additionalRequirements) { this.additionalRequirements = additionalRequirements; }
}
