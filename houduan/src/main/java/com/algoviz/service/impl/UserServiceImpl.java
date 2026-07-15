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

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);
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
    public void deleteUser(Integer id) {
        logger.info("删除用户：{}", id);
        userMapper.deleteById(id);
    }

    @Override
    public int countUsers() {
        return userMapper.countUsers();
    }
}
