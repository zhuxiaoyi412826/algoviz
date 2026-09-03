package com.algoviz.listener;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 会话超时监听器：Session 销毁时将用户标记为离线(1)
 * 覆盖场景：用户关闭浏览器、30分钟无操作超时、登出（session.invalidate 也会触发）
 * 注意：登出接口已主动置1，此处为兜底重复置1，无副作用
 */
@Component
public class UserSessionListener implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionListener.class);

    @Autowired
    private UserService userService;

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Object loginUser = session.getAttribute(AuthInterceptor.SESSION_USER);
        if (loginUser instanceof User) {
            Integer uid = ((User) loginUser).getId();
            if (uid != null) {
                try {
                    userService.updateLoginStatus(uid, 1);
                    logger.info("会话销毁，用户已标记离线: userId={}", uid);
                } catch (Exception e) {
                    logger.warn("会话销毁标记离线失败: userId={}, error={}", uid, e.getMessage());
                }
            }
        }
    }
}
