package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户模块访问统计（1:1 拆分自 user 表，user_id 主键）
 * 无行视为全 0，上报接口 upsert 自动建行
 */
@Schema(description = "用户模块访问统计")
@Data
public class UserVisitStat {
    @Schema(description = "用户ID（user.id，1:1）")
    private Long userId;
    @Schema(description = "逻辑删除冗余（与 user.is_deleted 同步）: 0=正常 1=已删除")
    private Integer isDeleted;
    @Schema(description = "AI对话次数")
    private Integer aiDialogues;
    @Schema(description = "数据结构访问次数")
    private Integer dsVisits;
    @Schema(description = "算法访问次数")
    private Integer algoVisits;
    @Schema(description = "OJ访问次数")
    private Integer ojVisits;
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;
    @Schema(description = "最后访问时间（任一模块上报时刷新）")
    private LocalDateTime lastVisitTime;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
