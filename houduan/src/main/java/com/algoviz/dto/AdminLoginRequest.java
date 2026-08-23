package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员登录请求")
public class AdminLoginRequest {
    @Schema(description = "管理员用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "登录地点")
    private String location;

    public AdminLoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
