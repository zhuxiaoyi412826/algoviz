package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "登录日志实体")
public class LoginLog {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "IP地址")
    private String ip;
    @Schema(description = "设备")
    private String device;
    @Schema(description = "登录地点")
    private String location;
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "失败原因")
    private String failReason;

    public LoginLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
