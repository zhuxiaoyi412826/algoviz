package com.algoviz.service.impl;

import com.algoviz.entity.User;
import com.algoviz.mapper.UserMapper;
import com.algoviz.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.algoviz.mapper.UserVisitStatMapper userVisitStatMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User findByUsernameIncludeDeleted(String username) {
        return userMapper.findByUsernameIncludeDeleted(username);
    }

    @Override
    public User findByEmailIncludeDeleted(String email) {
        return userMapper.findByEmailIncludeDeleted(email);
    }

    @Override
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        if (user.getLoginStatus() == null) {
            user.setLoginStatus(1);   // 新用户默认离线
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setIsDeleted(0);   // 新用户正常，逻辑删除标记固定为 0
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl("https://i.pravatar.cc/150?u=" + System.currentTimeMillis());
        }
        userMapper.insert(user);
        // 访问统计拆表：注册时初始化 user_visit_stat 行（幂等兜底，正常由 upsert 自动建行）
        if (user.getId() != null) {
            userVisitStatMapper.initForUser(user.getId().longValue());
        }
        return user;
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return user;
    }

    @Override
    public void updateLastLogin(Integer userId) {
        userMapper.updateLastLoginAt(userId);
    }

    @Override
    public List<User> getAllUsers() {
        logger.info("获取所有用户");
        return userMapper.getAllUsers();
    }

    @Override
    public List<User> getUsersByPage(int page, int pageSize) {
        logger.info("分页获取用户 - page: {}, pageSize: {}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.getUsersByPage(offset, pageSize);
    }

    @Override
    public User findById(Integer id) {
        logger.info("获取用户：{}", id);
        return userMapper.findById(id);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        logger.info("搜索用户：{}", keyword);
        return userMapper.searchUsers(keyword);
    }

    @Override
    public List<User> searchUsersByPage(String keyword, int page, int pageSize) {
        logger.info("分页搜索用户 - keyword: {}, page: {}, pageSize: {}", keyword, page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.searchUsersByPage(keyword, offset, pageSize);
    }

    @Override
    public int searchUsersCount(String keyword) {
        return userMapper.searchUsersCount(keyword);
    }

    @Override
    public void deleteUser(Integer id) {
        logger.info("删除用户：{}", id);
        userMapper.deleteById(id);
    }

    @Override
    public int countUsers() {
        return userMapper.countUsers();
    }

    @Override
    public List<User> getUsersByConditions(String keyword, Integer gender, Integer status, Integer loginStatus, String order, int page, int pageSize) {
        String safeOrder = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        int offset = (page - 1) * pageSize;
        return userMapper.getUsersByConditions(keyword, gender, status, loginStatus, safeOrder, offset, pageSize);
    }

    @Override
    public int getUsersCountByConditions(String keyword, Integer gender, Integer status, Integer loginStatus) {
        return userMapper.getUsersCountByConditions(keyword, gender, status, loginStatus);
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        userMapper.updateStatus(id, status);
    }

    @Override
    public int updatePassword(Integer id, String password) {
        return userMapper.updatePassword(id, password);
    }

    @Override
    public void updateLoginStatus(Integer id, Integer loginStatus) {
        userMapper.updateLoginStatus(id, loginStatus);
    }

    @Override
    public int cancelAccount(Integer id) {
        return userMapper.cancelAccount(id);
    }
}
