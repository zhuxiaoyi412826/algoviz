package com.algoviz.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Schema(description = "面试题目实体")
public class InterviewProblem {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "题目编号，唯一（如 MS001）")
    private String problemNo;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "难度：easy/medium/hard")
    private String difficulty;

    @Schema(description = "标签字符串（逗号分隔）")
    private String tags;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "题目描述（Markdown）")
    private String description;

    @Schema(description = "输入格式（Markdown）")
    private String inputFormat;

    @Schema(description = "输出格式（Markdown）")
    private String outputFormat;

    @Schema(description = "题解（Markdown）")
    private String solution;

    @Schema(description = "状态：ACTIVE/INACTIVE")
    private String status;

    @Schema(description = "是否高频：1=是 0=否")
    private Integer isFrequent;

    @Schema(description = "逻辑删除：1=已删除 0=正常")
    @JsonIgnore
    private Integer isDeleted;

    @Schema(description = "累计浏览数")
    private Integer viewCount;

    @Schema(description = "点赞数（冗余）")
    private Integer likeCount;

    @Schema(description = "点踩数（冗余）")
    private Integer dislikeCount;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "最后更新人")
    private String updatedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // === 扩展字段：前端展示用，不入库 ===
    @Schema(description = "难度中文：简单/中等/困难")
    public String getDifficultyLabel() {
        if (difficulty == null) return "中等";
        switch (difficulty.toLowerCase()) {
            case "easy": return "简单";
            case "hard": return "困难";
            default: return "中等";
        }
    }

    @Schema(description = "标签数组")
    public List<String> getTagList() {
        if (tags == null || tags.isEmpty()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** 兼容前端 tags 传数组或字符串 */
    @JsonSetter("tags")
    public void setTagsFromObject(Object tags) {
        if (tags == null) { this.tags = null; return; }
        if (tags instanceof String) {
            this.tags = (String) tags;
        } else if (tags instanceof List) {
            List<?> list = (List<?>) tags;
            StringBuilder sb = new StringBuilder();
            for (Object o : list) {
                String s = o == null ? "" : o.toString().trim();
                if (!s.isEmpty()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(s);
                }
            }
            this.tags = sb.toString();
        } else {
            this.tags = tags.toString();
        }
    }

    @JsonGetter("tags")
    public String getTagsString() {
        return this.tags;
    }
}
