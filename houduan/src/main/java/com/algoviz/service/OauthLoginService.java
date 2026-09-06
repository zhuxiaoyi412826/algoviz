package com.algoviz.service;

import com.algoviz.entity.User;

/**
 * 第三方授权登录服务（OAuth2 Client 成功回调后的本地账号映射）
 */
public interface OauthLoginService {

    /**
     * 第三方授权用户 -> 本地账号（绑定命中直接登录；未命中自动注册并建立绑定）
     *
     * @param provider   平台标识：github / gitee（与 user_oauth.provider 一致）
     * @param openId     第三方用户唯一 ID
     * @param login      第三方登录名（如 GitHub/Gitee 的 login，可为 null）
     * @param nickname   第三方昵称（可为 null，回退 login/openId）
     * @param avatarUrl  第三方头像（可为 null）
     * @param rawProfile 第三方 OAuth 返回完整资料 JSON 快照（可为 null，落 raw_profile 便于调试/扩展）
     * @return 可登录的本地 User（已通过 status/is_deleted 校验）
     * @throws com.algoviz.common.exception.BusinessException 绑定异常 / 账号被封禁注销 / 注册失败
     */
    User loginOrRegister(String provider, String openId, String login,
                         String nickname, String avatarUrl, String rawProfile);
}
