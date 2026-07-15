package com.algoviz.controller;

import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取用户列表", description = "获取所有用户或按关键词搜索")
    public Map<String, Object> getUsers(@RequestParam(required = false) String keyword) {
        logger.info("获取用户列表 - 关键词: {}", keyword);
        
        Map<String, Object> result = new HashMap<>();
        List<User> users;

        if (keyword != null && !keyword.isEmpty()) {
            users = userService.searchUsers(keyword);
        } else {
            users = userService.getAllUsers();
        }

        result.put("success", true);
        result.put("users", users);
        result.put("count", users.size());
        
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Map<String, Object> getUserById(@PathVariable Integer id) {
        logger.info("获取用户详情：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        User user = userService.findById(id);
        
        if (user != null) {
            result.put("success", true);
            result.put("user", user);
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        
        return result;
    }

    @PostMapping
    @Operation(summary = "添加用户", description = "添加新用户")
    public Map<String, Object> addUser(@RequestBody User user) {
        logger.info("添加用户：{}", user.getUsername());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查用户名是否已存在
            if (userService.findByUsername(user.getUsername()) != null) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }
            
            // 检查邮箱是否已存在
            if (userService.findByEmail(user.getEmail()) != null) {
                result.put("success", false);
                result.put("message", "邮箱已被使用");
                return result;
            }
            
            userService.createUser(user);
            result.put("success", true);
            result.put("message", "用户添加成功");
        } catch (Exception e) {
            logger.error("添加用户失败", e);
            result.put("success", false);
            result.put("message", "添加用户失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public Map<String, Object> updateUser(@PathVariable Integer id, @RequestBody User user) {
        logger.info("更新用户：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            user.setId(id);
            userService.updateUser(user);
            result.put("success", true);
            result.put("message", "用户更新成功");
        } catch (Exception e) {
            logger.error("更新用户失败", e);
            result.put("success", false);
            result.put("message", "更新用户失败：" + e.getMessage());
        }
        
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public Map<String, Object> deleteUser(@PathVariable Integer id) {
        logger.info("删除用户：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            User user = userService.findById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            userService.deleteUser(id);
            result.put("success", true);
            result.put("message", "用户删除成功");
        } catch (Exception e) {
            logger.error("删除用户失败", e);
            result.put("success", false);
            result.put("message", "删除用户失败：" + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/count")
    @Operation(summary = "获取用户数量", description = "获取用户总数")
    public Map<String, Object> getUserCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", userService.countUsers());
        return result;
    }
}