package com.algoviz.entity.rbac;

import lombok.Data;
import java.time.LocalDateTime;

/** 权限点表 sys_permission（按钮级权限） */
@Data
public class SysPermission {
    private Long id;
    private Long menuId;
    private String permCode;       // 权限编码如 content:ds:add
    private String permName;
    private Integer permType;      // 1-新增 2-编辑 3-删除 4-导出 5-审核 6-其他
    private String apiMethod;
    private String apiPath;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
