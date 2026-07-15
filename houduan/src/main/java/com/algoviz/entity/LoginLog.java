package com.algoviz.entity;

public class LoginLog {
    private String id;
    private String userId;
    private String username;
    private String ip;
    private String device;
    private String location;
    private String loginTime;
    private String status;
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
    public String getLoginTime() { return loginTime; }
    public void setLoginTime(String loginTime) { this.loginTime = loginTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
