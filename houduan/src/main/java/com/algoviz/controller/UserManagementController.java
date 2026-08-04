package com.algoviz.controller;

import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取用户列表，支持关键词搜索、性别/账号状态/登录状态筛选、注册时间排序")
    public Map<String, Object> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String loginStatus,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        logger.info("获取用户列表 - 关键词: {}, 性别: {}, 账号状态: {}, 登录状态: {}, 排序: {}, page: {}, pageSize: {}", keyword, gender, status, loginStatus, order, page, pageSize);

        Map<String, Object> result = new HashMap<>();
        List<User> users = userService.getUsersByConditions(keyword, gender, status, loginStatus, order, page, pageSize);
        int totalCount = userService.getUsersCountByConditions(keyword, gender, status, loginStatus);

        result.put("success", true);
        result.put("users", users);
        result.put("count", totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
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

    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "仅更新用户的账号状态（1:正常 0:封禁）")
    public Map<String, Object> updateUserStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                result.put("success", false);
                result.put("message", "状态值无效，只能为 0（封禁）或 1（正常）");
                return result;
            }
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            userService.updateStatus(id, status);
            result.put("success", true);
            result.put("message", "用户状态更新成功");
        } catch (Exception e) {
            logger.error("更新用户状态失败", e);
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
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

    @GetMapping("/export/json")
    @Operation(summary = "导出用户(JSON)", description = "导出所有用户数据为 JSON 格式文件（不含密码）")
    public ResponseEntity<byte[]> exportUsersAsJson() throws Exception {
        logger.info("导出用户数据为 JSON 文件");
        List<User> users = userService.getAllUsers();

        List<Map<String, Object>> safeUsers = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("age", u.getAge());
            map.put("gender", u.getGender());
            map.put("nickname", u.getNickname());
            map.put("avatarUrl", u.getAvatarUrl());
            map.put("loginStatus", u.getLoginStatus());
            map.put("status", u.getStatus());
            map.put("createdAt", u.getCreatedAt());
            map.put("updatedAt", u.getUpdatedAt());
            map.put("lastLoginAt", u.getLastLoginAt());
            safeUsers.add(map);
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportTime", java.time.LocalDateTime.now().toString());
        exportData.put("totalCount", safeUsers.size());
        exportData.put("users", safeUsers);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        byte[] contentBytes = json.getBytes(StandardCharsets.UTF_8);
        String filename = URLEncoder.encode("users.json", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_JSON)
                .body(contentBytes);
    }
}