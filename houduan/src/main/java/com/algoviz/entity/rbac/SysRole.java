package com.algoviz.entity.rbac;

import lombok.Data;
import java.time.LocalDateTime;

/** 角色表 sys_role */
@Data
public class SysRole {
    private Long id;
    private String roleCode;       // 角色编码 如 SUPER_ADMIN
    private String roleName;
    private Integer roleLevel;     // 1-超级 2-一级 3-二级
    private Long parentRoleId;
    private Integer dataScope;     // 1-全部 2-本部门 3-本人
    private String description;
    private Integer status;        // 0-禁用 1-正常
    private Integer isSystem;      // 0-否 1-系统内置
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
