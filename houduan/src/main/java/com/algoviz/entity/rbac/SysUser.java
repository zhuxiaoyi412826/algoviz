package com.algoviz.entity.rbac;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 后台管理员用户表 sys_user
 * 密码字段 VARCHAR(128)：
 *  - 超级管理员（algovize，id=1）：Argon2id 加密
 *  - 其他管理员：BCrypt 加密（VARCHAR(60) 也能放 128 字段里）
 */
@Data
public class SysUser {
    private Long id;
    private String username;       // VARCHAR(64) 登录账号
    private String password;       // VARCHAR(128) 加密密码
    private String realName;       // VARCHAR(64) 真实姓名
    private String email;
    private String phone;
    private String avatar;
    private Integer status;        // 0-禁用 1-正常 2-封禁
    private Integer accountType;   // 1-内部管理员 2-外部出题人
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
