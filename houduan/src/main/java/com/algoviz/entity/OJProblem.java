package com.algoviz.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

public class OJProblem {
    private Long id;
    private String problemNo;
    private String title;
    private String difficulty;
    private String tags;
    private String description;
    private String template;
    private String status;
    private Integer submissionCount;
    private Double acRate;
    private String createdAt;
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
    @JsonGetter("tags")
    public String getTags() { return tags; }

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
