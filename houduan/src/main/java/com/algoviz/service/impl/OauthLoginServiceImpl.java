package com.algoviz.service.impl;

import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.User;
import com.algoviz.entity.UserOauth;
import com.algoviz.mapper.UserOauthMapper;
import com.algoviz.service.AccountStatusService;
import com.algoviz.service.OauthLoginService;
import com.algoviz.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第三方授权登录核心实现
 *
 * <p>数据规则：</p>
 * <pre>
 *   1. user_oauth 命中 (provider, open_id) 且 bind_status=0 / is_deleted=0 → 登录并累计 login_count
 *   2. 未命中 → 自动注册：bind_scene=1，用户名 {provider}_{openId}，邮箱 {provider}_{openId}@oauth.local
 *   3. 冲突兜底：注册重名时追加 _1/_2... 后缀（最多 5 次）
 *   4. 第三方 token 列当前不持久化（业务暂不需要主动调用第三方 API）
 * </pre>
 */
@Service
public class OauthLoginServiceImpl implements OauthLoginService {

    private static final Logger log = LoggerFactory.getLogger(OauthLoginServiceImpl.class);

    /** 自动注册用户名/邮箱的最大重试次数（冲突时追加 _i 后缀） */
    private static final int MAX_RETRY = 5;

    /** 昵称/头像入库最大长度（对齐 user 表列宽，避免超长截断异常） */
    private static final int MAX_NICKNAME = 100;
    private static final int MAX_AVATAR = 500;
    /** raw_profile 快照上限（TEXT 单值建议上限），超出截断只保留头部 */
    private static final int MAX_RAW_PROFILE = 60000;

    /** bind_scene：1 注册自动绑定；2 用户手动账号绑定（后续迭代） */
    private static final int BIND_SCENE_AUTO_REGISTER = 1;

    @Autowired
    private UserOauthMapper userOauthMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountStatusService accountStatusService;

    @Override
    @Transactional
    public User loginOrRegister(String provider, String openId, String login,
                                String nickname, String avatarUrl, String rawProfile) {
        // 1. 已有有效绑定 → 直接登录（校验账号可用性）
        UserOauth bound = userOauthMapper.findByProviderAndOpenId(provider, openId);
        if (bound != null && bound.getUserId() != null) {
            User user = userService.findById(bound.getUserId());
            if (user == null) {
                throw new BusinessException("第三方账号绑定的本地账号不存在或已删除，请联系管理员");
            }
            assertActive(user);
            if (bound.getId() != null) {
                userOauthMapper.increaseLoginCount(bound.getId());   // 登录成功次数 +1
            }
            return user;
        }

        // 2. 未绑定 → 自动注册本地账号
        String nicknameCleaned = sanitizeText(nickname, login, openId, MAX_NICKNAME);
        String avatarCleaned = sanitizeText(avatarUrl, null, null, MAX_AVATAR);
        String rawCleaned = sanitizeText(rawProfile, null, null, MAX_RAW_PROFILE);

        User created = null;
        for (int i = 0; i < MAX_RETRY; i++) {
            String suffix = i == 0 ? openId : openId + "_" + i;
            String username = provider + "_" + suffix;         // 如 github_12345
            String email = username + "@oauth.local";          // 不与真实邮箱冲突

            // 含已注销/已删除也视为占用（用户名/邮箱永久占用，与注册查重口径一致）
            if (userService.findByUsernameIncludeDeleted(username) != null) {
                continue;
            }
            if (userService.findByEmailIncludeDeleted(email) != null) {
                continue;
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(null);          // 第三方账号无本地密码
            user.setNickname(nicknameCleaned);
            user.setAvatarUrl(avatarCleaned);
            created = userService.createUser(user);   // 内部补全 status=1 / is_deleted=0 / coins=1000 / stat 行
            break;
        }
        if (created == null || created.getId() == null) {
            throw new BusinessException("第三方账号自动注册失败，请稍后重试");
        }

        // 3. 建立绑定（UNIQUE(provider, open_id) 防并发重复绑定）
        UserOauth oauth = new UserOauth();
        oauth.setUserId(created.getId());
        oauth.setProvider(provider);
        oauth.setOpenId(openId);
        oauth.setBindScene(BIND_SCENE_AUTO_REGISTER);   // 注册自动绑定
        oauth.setNickname(nicknameCleaned);
        oauth.setAvatarUrl(avatarCleaned);
        oauth.setRawProfile(rawCleaned);
        try {
            userOauthMapper.insert(oauth);
        } catch (DuplicateKeyException e) {
            // 并发重复授权：绑定已被其他请求插入，退回命中路径（避免返回孤儿账号）
            log.warn("并发重复绑定，provider={}, openId={}，改走既有绑定", provider, openId);
            UserOauth again = userOauthMapper.findByProviderAndOpenId(provider, openId);
            if (again != null && again.getUserId() != null) {
                User user = userService.findById(again.getUserId());
                if (user != null) {
                    assertActive(user);
                    if (again.getId() != null) {
                        userOauthMapper.increaseLoginCount(again.getId());
                    }
                    return user;
                }
            }
            throw new BusinessException("第三方账号绑定冲突，请刷新后重试");
        }
        // 首次登录也计入 login_count（INSERT 默认 0，自增后 +1 = 1）
        if (oauth.getId() != null) {
            userOauthMapper.increaseLoginCount(oauth.getId());
        }
        return created;
    }

    /**
     * 账号可用性校验：封禁/永久注销抛业务异常；注销 15 天冷静期内本次第三方登录自动撤销注销并放行。
     */
    private void assertActive(User user) {
        AccountStatusService.LoginCheck chk = accountStatusService.checkForLogin(user);
        if (!chk.isAllowed()) {
            throw new BusinessException(chk.getRejectReason().contains("注销")
                    ? "该账号已注销，无法使用第三方登录"
                    : chk.getRejectReason());
        }
    }

    /** 清洗并截断文本（去掉换行/控制字符，防脏数据入库）；空值按 fallback 顺序回退 */
    private String sanitizeText(String value, String fallback1, String fallback2, int maxLen) {
        String s = value;
        if (s == null || s.trim().isEmpty()) {
            s = fallback1 != null ? fallback1 : fallback2;
        }
        if (s == null) {
            return null;
        }
        s = s.replaceAll("[\\r\\n\\t\\u0000-\\u001F]", " ").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
