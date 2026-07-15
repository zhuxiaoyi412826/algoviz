package com.algoviz.service.impl;

import com.algoviz.dto.LoginRequest;
import com.algoviz.dto.LoginResponse;
import com.algoviz.entity.User;
import com.algoviz.service.LoginService;
import com.algoviz.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserService userService;

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
            user.setAvatar("👤");
            user.setGender("未知");
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
        userInfo.setAvatar(user.getAvatar());
        userInfo.setNickname(user.getNickname());
        response.setUserInfo(userInfo);

        return response;
    }
}
