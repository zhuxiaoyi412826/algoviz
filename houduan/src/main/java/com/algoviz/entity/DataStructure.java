package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "数据结构实体")
public class DataStructure {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private String createdAt;
    @Schema(description = "更新时间")
    private String updatedAt;

    public DataStructure() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}