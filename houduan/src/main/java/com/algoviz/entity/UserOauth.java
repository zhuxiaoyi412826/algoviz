package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方授权绑定实体（user_oauth 表）
 *
 * <p>通用模板结构（与运行库保持一致，编码仅读写下列明确存在的列）：</p>
 * <pre>
 *   id          BIGINT  AUTO_INCREMENT 主键
 *   user_id     BIGINT  绑定的本地 user.id（NULL=尚未绑定）
 *   provider    VARCHAR 第三方平台标识：github / gitee
 *   open_id     VARCHAR 第三方平台用户唯一 ID
 *   union_id    VARCHAR 第三方平台开放平台统一 ID（可能为 NULL，未使用则不读写）
 *   nickname    VARCHAR 第三方平台昵称（快照，用于首次自动注册回填）
 *   avatar_url  VARCHAR 第三方平台头像（快照）
 *   created_at  DATETIME 绑定时间
 *   UNIQUE(provider, open_id)
 * </pre>
 *
 * <p>设计约束：本实体不携带明文 access_token 等敏感凭据，避免落库扩大泄露面；
 * 若实际表包含 token 相关列，也不在此实体中读写（授权凭据仅存在于内存 OAuth2 会话中）。</p>
 */
@Data
public class UserOauth {

    private Long id;

    /** 绑定的本地用户 ID（user.id） */
    private Integer userId;

    /** 第三方平台标识：github / gitee */
    private String provider;

    /** 第三方平台用户唯一 ID */
    private String openId;

    /** 第三方平台昵称快照 */
    private String nickname;

    /** 第三方平台头像快照 */
    private String avatarUrl;

    /** 绑定时间 */
    private LocalDateTime createdAt;
}
