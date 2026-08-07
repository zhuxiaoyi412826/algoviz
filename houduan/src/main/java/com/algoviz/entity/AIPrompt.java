package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI提示词实体")
public class AIPrompt {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "使用次数")
    private Integer usageCount;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private String createdAt;

    public AIPrompt() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}