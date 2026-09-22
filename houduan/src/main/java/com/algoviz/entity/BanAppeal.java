package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 封禁申诉实体（表 ban_appeal）。
 *
 * <p>被封禁用户（user.status=0）无法登录，因此在登录页提供独立申诉入口：
 * 填写「账号 + 注册邮箱 + 邮箱验证码 + 申诉理由」提交，邮箱必须与账号注册邮箱一致，
 * 以确认申诉人确实是该账号的持有者。</p>
 *
 * <p>状态机：pending（待处理）→ approved（已通过，后台处理时自动把 status 置回 1 解封）
 * / rejected（已驳回）。</p>
 */
@Schema(description = "封禁申诉实体")
@Data
public class BanAppeal {

    /** 申诉状态：待处理 */
    public static final String STATUS_PENDING = "pending";
    /** 申诉状态：已通过（并自动解封） */
    public static final String STATUS_APPROVED = "approved";
    /** 申诉状态：已驳回 */
    public static final String STATUS_REJECTED = "rejected";

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "被申诉用户ID")
    private Long userId;
    @Schema(description = "被封禁账号（用户名）")
    private String username;
    @Schema(description = "申诉邮箱（须与账号注册邮箱一致）")
    private String email;
    @Schema(description = "申诉理由")
    private String reason;
    @Schema(description = "状态: pending=待处理 approved=已通过 rejected=已驳回")
    private String status;
    @Schema(description = "管理员处理回复")
    private String reply;
    @Schema(description = "处理人后台账号ID")
    private Long handlerId;
    @Schema(description = "处理人后台账号名")
    private String handlerName;
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;
    @Schema(description = "提交时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}