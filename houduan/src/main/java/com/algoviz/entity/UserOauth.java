package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方授权绑定实体（user_oauth 表）
 *
 * <p>通用模板结构（与运行库保持一致，编码仅读写下列明确存在的列）：</p>
 * <pre>
 *   id                    BIGINT  主键
 *   user_id               BIGINT  绑定的本地 user.id
 *   provider              VARCHAR 平台标识：github / gitee
 *   open_id               VARCHAR 第三方用户唯一 ID
 *   bind_scene            TINYINT 绑定场景：1 注册自动绑定  2 用户手动账号绑定
 *   bind_status           TINYINT 绑定状态：0 正常 1 授权已撤销 2 授权过期
 *   login_count           INT     该绑定累计登录成功次数
 *   nickname / avatar_url VARCHAR 第三方资料快照
 *   access_token / refresh_token / token_encrypted / expires_at / last_token_refresh_time
 *                         （预留：当前业务不持久化第三方 token，均为 NULL；接入主动调用/续期时再启用，token 需加密）
 *   raw_profile           TEXT    OAuth 授权时第三方完整 JSON 快照（调试/扩展免重调）
 *   is_deleted            TINYINT 逻辑删除/解绑：0 正常 1 解绑删除
 *   created_at / updated_at
 *   UNIQUE(provider, open_id)
 * </pre>
 *
 * <p>设计约束：</p>
 * <ul>
 *   <li>查询绑定一律过滤 is_deleted=0；登录仅接受 bind_status=0（授权撤销/过期走重新授权）；</li>
 *   <li>若将来持久化第三方 token，必须先加密（token_encrypted=1），禁止明文落库；</li>
 *   <li>敏感凭据默认不入内存会话。</li>
 * </ul>
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

    /** 绑定场景：1 注册自动绑定  2 用户手动账号绑定 */
    private Integer bindScene;

    /** 绑定状态：0 正常  1 授权已撤销  2 授权过期 */
    private Integer bindStatus;

    /** 该绑定累计登录成功次数 */
    private Integer loginCount;

    /** 第三方昵称快照 */
    private String nickname;

    /** 第三方头像快照 */
    private String avatarUrl;

    /** 加密存储的 access_token（当前业务不持久化，预留） */
    private String accessToken;

    /** 加密存储的刷新令牌（预留） */
    private String refreshToken;

    /** token 字段是否加密：0 未加密  1 已加密 */
    private Integer tokenEncrypted;

    /** access_token 过期时间 */
    private LocalDateTime expiresAt;

    /** token 最后刷新时间 */
    private LocalDateTime lastTokenRefreshTime;

    /** OAuth 授权时第三方返回的完整 JSON 快照 */
    private String rawProfile;

    /** 逻辑删除/解绑：0 正常  1 解绑删除 */
    private Integer isDeleted;

    /** 绑定时间 */
    private LocalDateTime createdAt;
}
