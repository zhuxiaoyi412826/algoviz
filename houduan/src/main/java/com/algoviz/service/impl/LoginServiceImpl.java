package com.algoviz.service.impl;

import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.LoginLockService;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import com.algoviz.common.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginServiceImpl implements LoginService {

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
            // 创建新用户
            user = new User();
            user.setUsername(openId);
            user.setEmail(openId + "@example.com");
            user.setPassword(""); // 微信登录不需要密码
            user.setAge(null);
            user.setGender("未知");
            user.setLoginStatus("offline");
            user.setStatus(1);
            user.setAvatarUrl("https://i.pravatar.cc/150?u=" + System.currentTimeMillis());
            user.setNickname("微信用户" + new Random().nextInt(10000));
            user = userService.createUser(user);
        } else {
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

        // 3) 校验密码（MD5加密比对）
        String inputMd5 = PasswordEncoderUtil.md5Encode(password);
        boolean pwOk = inputMd5.equals(user.getPassword()) || password.equals(user.getPassword());
        if (!pwOk) {
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, lockId);
            response.setSuccess(false);
            response.setMessage("用户不存在或密码错误");
            return response;
        }

        // 4) 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            response.setSuccess(false);
            response.setMessage("账号已被禁用，请联系管理员");
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

        // 3) 校验密码
        String inputMd5 = PasswordEncoderUtil.md5Encode(password);
        boolean pwOk = inputMd5.equals(user.getPassword()) || password.equals(user.getPassword());
        if (!pwOk) {
            loginLockService.recordFailure(LoginLockService.LoginLockType.USER, lockId);
            response.setSuccess(false);
            response.setMessage("邮箱未注册或密码错误");
            return response;
        }

        // 4) 账号禁用
        if (user.getStatus() != null && user.getStatus() == 0) {
            response.setSuccess(false);
            response.setMessage("账号已被禁用，请联系管理员");
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
