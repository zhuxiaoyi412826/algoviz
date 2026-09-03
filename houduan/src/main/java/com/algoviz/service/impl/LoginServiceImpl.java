package com.algoviz.service.impl;

import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.LoginLockService;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import com.algoviz.common.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private LoginLockService loginLockService;

    // 存储验证码，Key: 验证码, Value: openId（为空说明还未扫码）
    private Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    @Override
    public LoginResponse login(LoginRequest request) {
        // 这个原有的 login 接口其实可以不用了，或者保留作备用
        return checkLoginStatus(request.getVerificationCode());
    }

    @Override
    public String generateVerificationCode() {
        // 生成6位数字验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        // 存储验证码，初始 openId 为空
        verificationCodes.put(code, "");
        return code;
    }

    @Override
    public boolean validateVerificationCode(String code) {
        return verificationCodes.containsKey(code);
    }

    @Override
    public boolean verifyCodeFromWechat(String code, String openId) {
        if (verificationCodes.containsKey(code)) {
            verificationCodes.put(code, openId);
            return true;
        }
        return false;
    }

    @Override
    public LoginResponse checkLoginStatus(String code) {
        LoginResponse response = new LoginResponse();
        if (!verificationCodes.containsKey(code)) {
            response.setSuccess(false);
            response.setMessage("验证码无效或已过期");
            return response;
        }

        String openId = verificationCodes.get(code);
        if (openId == null || openId.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("等待扫码");
            return response;
        }

        // 已经扫码成功，进行登录或注册
        User user = userService.findByUsername(openId);

        if (user == null) {
            // 含已逻辑删除/注销账号也要拦截：防止唯一索引冲突，且墓碑账号不可恢复登录
            User tombstone = userService.findByUsernameIncludeDeleted(openId);
            if (tombstone != null) {
                response.setSuccess(false);
                if (tombstone.getIsDeleted() != null && tombstone.getIsDeleted() == 1) {
                    response.setMessage("账号不存在");
                } else if (tombstone.getStatus() != null && tombstone.getStatus() == -1) {
                    response.setMessage("账号已注销");
                } else if (tombstone.getStatus() != null && tombstone.getStatus() == 0) {
                    response.setMessage("账号已被禁用，请联系管理员");
                } else {
                    response.setMessage("账号异常，请联系管理员");
                }
                verificationCodes.remove(code);
                return response;
            }
            // 创建新用户
            user = new User();
            user.setUsername(openId);
            user.setEmail(openId + "@example.com");
            user.setPassword(""); // 微信登录不需要密码
            user.setAge(null);
            user.setGender(null);   // 性别未知（1=男 0=女）
            user.setLoginStatus(1);   // 新用户默认离线（扫码登录成功即置0在线）
            user.setStatus(1);
            user.setAvatarUrl("https://i.pravatar.cc/150?u=" + System.currentTimeMillis());
            user.setNickname("微信用户" + new Random().nextInt(10000));
            user = userService.createUser(user);
        } else {
            // 状态校验：注销/封禁禁止登录
            String statusError = checkAccountStatus(user);
            if (statusError != null) {
                response.setSuccess(false);
                response.setMessage(statusError);
                verificationCodes.remove(code);
                return response;
            }
            // 更新最后登录时间
            userService.updateLastLogin(user.getId());
        }

        // 验证通过，从map中移除
        verificationCodes.remove(code);

        // 生成token（实际项目中应该使用JWT）
        String token = "wx_token_" + System.currentTimeMillis();

        // 构建响应
        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setAge(user.getAge());
        userInfo.setNickname(user.getNickname());
        response.setUserInfo(userInfo);

        return response;
    }

    /**
     * 账号状态校验：返回 null 表示可登录，否则返回拒绝原因
     * status: 1=正常 0=封禁 -1=注销（is_deleted=1 的账号在查询层已被过滤）
     */
    private String checkAccountStatus(User user) {
        if (user == null) return null;
        Integer st = user.getStatus();
        if (st != null && st == -1) {
            return "账号已注销";
        }
        if (st != null && st == 0) {
            return "账号已被禁用，请联系管理员";
        }
        return null;
    }

    /**
     * 密码升级：若用户密码仍为弱算法（MD5/明文）存储，登录成功后自动重哈希为 BCrypt（平滑迁移）
     */
    private void upgradePasswordIfNeeded(User user, String rawPassword) {
        String stored = user.getPassword();
        boolean isWeak = stored != null
                && !stored.startsWith("$2a$") && !stored.startsWith("$2b$")
                && !stored.startsWith("$2y$") && !stored.startsWith("$argon2id$");
        if (isWeak) {
            String newHash = PasswordEncoderUtil.bcryptEncode(rawPassword);
            userService.updatePassword(user.getId(), newHash);
            user.setPassword(newHash);
            logger.info("用户 {} 密码已从弱算法升级为 BCrypt", user.getUsername());
        }
    }

    /**
     * 密码校验 + 弱算法升级（登录用）：
     *  ① 标准哈希（Argon2id / BCrypt / MD5）→ autoMatches 校验
     *  ② 历史明文存储 → equals 兼容一次并记录告警
     *  校验通过即触发弱算法升级（MD5/明文→BCrypt），避免"明文账号永远登不进、升不了级"的死锁。
     */
    private boolean checkPasswordWithLegacyUpgrade(User user, String rawPassword) {
        boolean pwOk = PasswordEncoderUtil.autoMatches(rawPassword, user.getPassword());
        if (!pwOk && rawPassword.equals(user.getPassword())) {
            logger.warn("用户 {} 使用明文密码登录成功（历史遗留数据），已触发 BCrypt 升级", user.getUsername());
            pwOk = true;
        }
        if (pwOk) {
            upgradePasswordIfNeeded(user, rawPassword);
        }
        return pwOk;
    }

    @Override
    public LoginResponse loginByAccount(String username, String password) {
        LoginResponse response = new LoginResponse();

        if (username == null || username.trim().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("用户名不能为空");
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("密码不能为空");
            return response;
        }
        final String identifier = username.trim();

        // 1) 前置检查：账号是否已被锁定（对不存在的用户也按 username 做同样的键，防止枚举）
        LoginLockService.LockStatus ls = loginLockService.checkLock(LoginLockService.LoginLockType.USER, identifier);
        if (ls.locked) {
            response.setSuccess(false);
            String msg = "登录失败次数过多，账号已锁定，剩余 " + loginLockService.formatRemaining(ls.expireAtMs);
            response.setMessage(msg);
            return response;
        }

        // 2) 根据用户名查询用户
        User user = userService.findByUsername(identifier);
        if (user == null) {
            // 用户不存在也计入失败，防止暴力枚举用户名（对 username 作为锁定key）
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, identifier);
            response.setSuccess(false);
            response.setMessage("用户不存在或密码错误");
            return response;
        }
        // 以用户实体 username（规范化值）作为真实锁标识
        final String lockId = user.getUsername();

        // 3) 校验密码：自动探测存储算法（Argon2id/BCrypt/MD5）；历史明文账号兼容一次并自动升级
        if (!checkPasswordWithLegacyUpgrade(user, password)) {
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, lockId);
            response.setSuccess(false);
            response.setMessage("用户不存在或密码错误");
            return response;
        }

        // 4) 检查用户状态：注销(-1) / 封禁(0)
        String statusError = checkAccountStatus(user);
        if (statusError != null) {
            response.setSuccess(false);
            response.setMessage(statusError);
            return response;
        }

        // 5) 密码正确：清空失败计数和锁定键
        loginLockService.reset(LoginLockService.LoginLockType.USER, lockId);

        // 6) 更新最后登录时间
        userService.updateLastLogin(user.getId());

        // 7) 生成token
        String token = "account_token_" + System.currentTimeMillis();

        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setAge(user.getAge());
        userInfo.setNickname(user.getNickname());
        response.setUserInfo(userInfo);

        return response;
    }

    @Override
    public LoginResponse loginByEmail(String email, String password) {
        LoginResponse response = new LoginResponse();

        if (email == null || email.trim().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("邮箱不能为空");
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("密码不能为空");
            return response;
        }
        final String emailTrimmed = email.trim();

        // 1) 前置锁定检查
        LoginLockService.LockStatus ls = loginLockService.checkLock(LoginLockService.LoginLockType.USER, emailTrimmed);
        if (ls.locked) {
            response.setSuccess(false);
            response.setMessage("登录失败次数过多，账号已锁定，剩余 " + loginLockService.formatRemaining(ls.expireAtMs));
            return response;
        }

        // 2) 查询用户
        User user = userService.findByEmail(emailTrimmed);
        if (user == null) {
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, emailTrimmed);
            response.setSuccess(false);
            response.setMessage("邮箱未注册或密码错误");
            return response;
        }
        final String lockId = user.getUsername();
        // 对锁定态做二次检查（按真实 username 键）
        LoginLockService.LockStatus ls2 = loginLockService.checkLock(LoginLockService.LoginLockType.USER, lockId);
        if (ls2.locked) {
            response.setSuccess(false);
            response.setMessage("登录失败次数过多，账号已锁定，剩余 " + loginLockService.formatRemaining(ls2.expireAtMs));
            return response;
        }

        // 3) 校验密码：自动探测存储算法（Argon2id/BCrypt/MD5）；历史明文账号兼容一次并自动升级
        if (!checkPasswordWithLegacyUpgrade(user, password)) {
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, lockId);
            response.setSuccess(false);
            response.setMessage("邮箱未注册或密码错误");
            return response;
        }

        // 4) 账号状态校验：注销(-1) / 封禁(0)
        String statusError = checkAccountStatus(user);
        if (statusError != null) {
            response.setSuccess(false);
            response.setMessage(statusError);
            return response;
        }

        // 5) 密码正确：清零
        loginLockService.reset(LoginLockService.LoginLockType.USER, lockId);

        userService.updateLastLogin(user.getId());

        String token = "email_token_" + System.currentTimeMillis();

        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setAge(user.getAge());
        userInfo.setNickname(user.getNickname());
        response.setUserInfo(userInfo);

        return response;
    }
}
