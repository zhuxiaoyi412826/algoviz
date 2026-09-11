package com.algoviz.service;

import com.algoviz.entity.User;

import java.util.List;

public interface UserService {
    User findByUsername(String username);
    User findByEmail(String email);
    /** 注册查重专用：含已逻辑删除/注销账号（用户名/邮箱永久占用） */
    User findByUsernameIncludeDeleted(String username);
    User findByEmailIncludeDeleted(String email);
    User createUser(User user);
    User updateUser(User user);
    /** 个人中心：修改昵称/邮箱/性别/头像（校验在 Controller 层完成） */
    void updateProfile(Integer id, String nickname, String email, Integer gender, String avatarUrl);
    void updateLastLogin(Integer userId);

    List<User> getAllUsers();
    List<User> getUsersByPage(int page, int pageSize);
    User findById(Integer id);
    List<User> searchUsers(String keyword);
    List<User> searchUsersByPage(String keyword, int page, int pageSize);
    int searchUsersCount(String keyword);
    void deleteUser(Integer id);
    int countUsers();
    List<User> getUsersByConditions(String keyword, Integer gender, Integer status, Integer loginStatus, String order, int page, int pageSize);
    int getUsersCountByConditions(String keyword, Integer gender, Integer status, Integer loginStatus);
    void updateStatus(Integer id, Integer status);
    int updatePassword(Integer id, String password);
    void updateLoginStatus(Integer id, Integer loginStatus);
    /** 提交注销申请：status=-1、记录 cancel_at（15天冷静期起点）并强制下线（数据保留，后台仍可见） */
    int cancelAccount(Integer id);
}
