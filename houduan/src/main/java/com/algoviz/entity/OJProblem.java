package com.algoviz.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "OJ题目实体")
public class OJProblem {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "题目编号")
    private String problemNo;
    @Schema(description = "题目标题")
    private String title;
    @Schema(description = "难度")
    private String difficulty;
    @Schema(description = "标签")
    private String tags;
    @Schema(description = "题目描述")
    private String description;
    @Schema(description = "代码模板")
    private String template;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "提交次数")
    private Integer submissionCount;
    @Schema(description = "通过率")
    private Double acRate;
    @Schema(description = "创建时间")
    private String createdAt;
    @Schema(description = "更新时间")
    private String updatedAt;

    public OJProblem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProblemNo() { return problemNo; }
    public void setProblemNo(String problemNo) { this.problemNo = problemNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    @Schema(description = "标签")
    @JsonGetter("tags")
    public String getTags() { return tags; }

    @Schema(description = "标签")
    @JsonSetter("tags")
    public void setTags(Object tags) {
        if (tags == null) {
            this.tags = null;
        } else if (tags instanceof String) {
            this.tags = (String) tags;
        } else if (tags instanceof List) {
            List<?> list = (List<?>) tags;
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                String s = item == null ? "" : item.toString().trim();
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(Integer submissionCount) { this.submissionCount = submissionCount; }
    public Double getAcRate() { return acRate; }
    public void setAcRate(Double acRate) { this.acRate = acRate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}