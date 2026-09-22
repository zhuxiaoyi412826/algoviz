package com.algoviz.service;

import com.algoviz.entity.User;
import com.algoviz.entity.UserOauth;

import java.util.List;

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

    /**
     * 将第三方账号绑定到已登录的本地账号（bind_scene=2，手动绑定场景）。
     *
     * <p>约束：</p>
     * <ul>
     *   <li>该 (provider, openId) 不能已绑定到其他本地账号（UNIQUE 约束 + 主动校验，防止账号被他人抢走）；</li>
     *   <li>同一用户同一平台不可重复绑定（先解绑再绑）；</li>
     *   <li>本地账号必须处于正常状态（status=1）。</li>
     * </ul>
     *
     * @return 新建的绑定记录
     */
    UserOauth bindToExistingAccount(Integer userId, String provider, String openId,
                                    String nickname, String avatarUrl, String rawProfile);

    /** 查询某用户的全部有效第三方绑定 */
    List<UserOauth> listBindings(Integer userId);

    /**
     * 解绑：将某用户某平台的有效绑定逻辑删除（is_deleted=1）。
     * @return true=解绑成功；false=未找到可解绑记录
     */
    boolean unbind(Integer userId, String provider);
}
