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
    User findById(Integer id);
    List<User> searchUsers(String keyword);
    void deleteUser(Integer id);
    int countUsers();
}
