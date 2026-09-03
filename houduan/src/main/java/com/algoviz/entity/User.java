package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户实体")
@Data
public class User {
    @Schema(description = "主键ID")
    private Integer id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号（预留，暂不启用）")
    private String phone;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "年龄")
    private Integer age;
    @Schema(description = "性别: 1=男 0=女")
    private Integer gender;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像地址")
    private String avatarUrl;
    @Schema(description = "登录状态: 0=在线 1=离线")
    private Integer loginStatus;
    @Schema(description = "状态: 1=正常 0=封禁 -1=注销")
    private Integer status;
    @Schema(description = "逻辑删除: 0=正常 1=已删除")
    private Integer isDeleted;
    @Schema(description = "硬币余额")
    private Integer coins;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;
}