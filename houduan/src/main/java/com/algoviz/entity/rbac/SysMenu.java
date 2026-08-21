package com.algoviz.entity.rbac;

import lombok.Data;
import java.time.LocalDateTime;

/** 菜单表 sys_menu（目录/菜单/按钮） */
@Data
public class SysMenu {
    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;      // 1-目录 2-菜单 3-按钮
    private String path;
    private String component;
    private String perms;          // 权限标识如 content:ds:list
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
