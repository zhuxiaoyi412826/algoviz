package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "操作日志实体")
public class OperationLog {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "模块")
    private String module;
    @Schema(description = "操作")
    private String action;
    @Schema(description = "详情")
    private String detail;
    @Schema(description = "IP地址")
    private String ip;
    @Schema(description = "创建时间")
    private String createdAt;

    public OperationLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}