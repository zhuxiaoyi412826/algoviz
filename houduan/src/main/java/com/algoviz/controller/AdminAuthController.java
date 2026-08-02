package com.algoviz.controller;

import com.algoviz.dto.AdminLoginRequest;
import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.Admin;
import com.algoviz.entity.LoginLog;
import com.algoviz.mapper.AdminMapper;
import com.algoviz.mapper.LoginLogMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "管理员认证", description = "管理员登录与账号CRUD")
public class AdminAuthController {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@RequestBody AdminLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String device = httpRequest.getHeader("User-Agent");

        Admin admin = adminMapper.findByUsername(request.getUsername());
        if (admin == null) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId("unknown");
            failLog.setUsername(request.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("用户名不存在");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("用户名或密码错误");
        }

        if (!"admin123".equals(request.getPassword())) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(admin.getId());
            failLog.setUsername(admin.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("密码错误");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("用户名或密码错误");
        }

        if ("disabled".equals(admin.getStatus())) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(admin.getId());
            failLog.setUsername(admin.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("账号已禁用");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("账号已被禁用");
        }

        adminMapper.updateLastLogin(admin.getId(), ip);
        LoginLog successLog = new LoginLog();
        successLog.setId(UUID.randomUUID().toString());
        successLog.setUserId(admin.getId());
        successLog.setUsername(admin.getUsername());
        successLog.setIp(ip);
        successLog.setDevice(device);
        successLog.setStatus("success");
        loginLogMapper.insert(successLog);

        String token = UUID.randomUUID().toString();
        admin.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", admin);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/info")
    public ApiResponse<Admin> getAdminInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Admin admin = adminMapper.findById("1");
        if (admin != null) {
            admin.setPassword(null);
        }
        return ApiResponse.success(admin);
    }

    @PostMapping("/admin/logout")
    public ApiResponse<Void> adminLogout() {
        return ApiResponse.success(null);
    }

    @GetMapping("/system/admin")
    public ApiResponse<Map<String, Object>> getAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Admin> list = adminMapper.findByPage(offset, pageSize);
        list.forEach(admin -> admin.setPassword(null));
        int total = adminMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/system/admin")
    public ApiResponse<Admin> createAdmin(@RequestBody Admin admin) {
        admin.setId(UUID.randomUUID().toString());
        admin.setPassword("admin123");
        admin.setStatus("active");
        adminMapper.insert(admin);

        Admin created = adminMapper.findById(admin.getId());
        if (created != null) {
            created.setPassword(null);
        }
        return ApiResponse.success(created);
    }

    @PutMapping("/system/admin/{id}")
    public ApiResponse<Admin> updateAdmin(@PathVariable String id, @RequestBody Admin admin) {
        admin.setId(id);
        adminMapper.update(admin);

        Admin updated = adminMapper.findById(id);
        if (updated != null) {
            updated.setPassword(null);
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/system/admin/{id}")
    public ApiResponse<Void> deleteAdmin(@PathVariable String id) {
        boolean success = adminMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @PostMapping("/system/admin/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable String id) {
        boolean success = adminMapper.updatePassword(id, "admin123") > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("重置密码失败");
    }

    @PutMapping("/system/admin/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        boolean success = adminMapper.updateStatus(id, body.get("status")) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("状态变更失败");
    }
}