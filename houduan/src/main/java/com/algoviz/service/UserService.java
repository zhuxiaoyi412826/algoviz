package com.algoviz.service;

import com.algoviz.entity.User;

import java.util.List;

public interface UserService {
    User findByUsername(String username);
    User findByEmail(String email);
    User createUser(User user);
    User updateUser(User user);
    void updateLastLogin(Integer userId);
    
    List<User> getAllUsers();
    List<User> getUsersByPage(int page, int pageSize);
    User findById(Integer id);
    List<User> searchUsers(String keyword);
    List<User> searchUsersByPage(String keyword, int page, int pageSize);
    int searchUsersCount(String keyword);
    void deleteUser(Integer id);
    int countUsers();
    List<User> getUsersByConditions(String keyword, String gender, Integer status, String loginStatus, String order, int page, int pageSize);
    int getUsersCountByConditions(String keyword, String gender, Integer status, String loginStatus);
    void updateStatus(Integer id, Integer status);
    int updatePassword(Integer id, String password);
}
