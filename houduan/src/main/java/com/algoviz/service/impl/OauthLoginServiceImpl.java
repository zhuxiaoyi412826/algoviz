package com.algoviz.service.impl;

import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.User;
import com.algoviz.entity.UserOauth;
import com.algoviz.mapper.UserOauthMapper;
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
 *   1. user_oauth 命中 (provider, open_id) → 取绑定的 user_id 登录（校验 status / is_deleted）
 *   2. 未命中 → 自动注册：用户名 = {provider}_{openId}，邮箱 = {provider}_{openId}@oauth.local
 *      （前缀 + 数字 ID 跨平台不冲突，且与真实用户命名空间隔离）
 *   3. 冲突兜底：注册重名时追加 _1/_2... 后缀（最多 5 次）
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

    @Autowired
    private UserOauthMapper userOauthMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public User loginOrRegister(String provider, String openId, String login,
                                String nickname, String avatarUrl) {
        // 1. 已有绑定 → 直接登录（校验账号可用性）
        UserOauth bound = userOauthMapper.findByProviderAndOpenId(provider, openId);
        if (bound != null && bound.getUserId() != null) {
            User user = userService.findById(bound.getUserId());
            if (user == null) {
                throw new BusinessException("第三方账号绑定的本地账号不存在或已删除，请联系管理员");
            }
            assertActive(user);
            return user;
        }

        // 2. 未绑定 → 自动注册本地账号
        String nicknameCleaned = sanitizeText(nickname, login, openId, MAX_NICKNAME);
        String avatarCleaned = sanitizeText(avatarUrl, null, null, MAX_AVATAR);

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
        oauth.setNickname(nicknameCleaned);
        oauth.setAvatarUrl(avatarCleaned);
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
                    return user;
                }
            }
            throw new BusinessException("第三方账号绑定冲突，请刷新后重试");
        }
        return created;
    }

    /** 账号可用性校验：1=正常可用；0=封禁；-1=注销（null 视为正常） */
    private void assertActive(User user) {
        Integer st = user.getStatus();
        if (st != null && st == -1) {
            throw new BusinessException("该账号已注销，无法使用第三方登录");
        }
        if (st != null && st == 0) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
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
