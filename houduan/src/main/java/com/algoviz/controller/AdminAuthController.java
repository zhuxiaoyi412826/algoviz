package com.algoviz.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.algoviz.dto.AdminLoginRequest;
import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.LoginLog;
import com.algoviz.entity.rbac.SysRole;
import com.algoviz.entity.rbac.SysUser;
import com.algoviz.mapper.LoginLogMapper;
import com.algoviz.mapper.rbac.SysRoleMapper;
import com.algoviz.mapper.rbac.SysUserMapper;
import com.algoviz.common.util.PasswordEncoderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "管理员认证", description = "管理员登录与账号CRUD（Sa-Token + 三层密码加密）")
public class AdminAuthController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * 后台管理员登录（Sa-Token）
     *  密码校验：
     *    - 用户ID=1 或 username=algovize → Argon2id 校验
     *    - 其他 sys_user 管理员 → BCrypt 校验
     *  返回：Sa-Token Token 字符串 + 用户信息 + 角色列表
     */
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@RequestBody AdminLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String device = httpRequest.getHeader("User-Agent");

        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null) {
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

        // 三层密码校验：超级管理员(id=1 / algovize) Argon2id；其他管理员 BCrypt
        boolean pwOk;
        if (user.getId() == 1L || "algovize".equalsIgnoreCase(user.getUsername())) {
            pwOk = PasswordEncoderUtil.argon2Matches(request.getPassword(), user.getPassword());
        } else {
            pwOk = PasswordEncoderUtil.bcryptMatches(request.getPassword(), user.getPassword());
        }
        if (!pwOk) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(String.valueOf(user.getId()));
            failLog.setUsername(user.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("密码错误");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(String.valueOf(user.getId()));
            failLog.setUsername(user.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("账号已禁用");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("账号已被禁用");
        }

        // 更新登录时间
        sysUserMapper.updateLastLogin(user.getId(), LocalDateTime.now(), ip);
        LoginLog successLog = new LoginLog();
        successLog.setId(UUID.randomUUID().toString());
        successLog.setUserId(String.valueOf(user.getId()));
        successLog.setUsername(user.getUsername());
        successLog.setIp(ip);
        successLog.setDevice(device);
        successLog.setStatus("success");
        loginLogMapper.insert(successLog);

        // Sa-Token 登录（loginId = sys_user.id）
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 返回：角色码 + 权限码 + 菜单
        List<String> roles = sysUserMapper.findRoleCodesByUserId(user.getId());
        List<String> perms;
        if (user.getId() == 1L) {
            perms = List.of("*");
        } else {
            perms = sysUserMapper.findPermissionCodesByUserId(user.getId());
        }
        List<?> menus = user.getId() == 1L
                ? sysRoleMapper.findAllMenus()
                : sysRoleMapper.findMenusByUserId(user.getId());

        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenName", StpUtil.getTokenName());
        result.put("loginId", user.getId());
        result.put("userInfo", user);
        result.put("roles", roles);
        result.put("permissions", perms);
        result.put("menus", menus);
        return ApiResponse.success(result);
    }

    /**
     * 根据当前 Sa-Token Token 获取管理员信息
     * 如果未登录或 token 无效，返回 401 错误（前端据此跳转登录页）
     */
    @GetMapping("/admin/info")
    public ApiResponse<Map<String, Object>> getAdminInfo() {
        try {
            if (!StpUtil.isLogin()) {
                return ApiResponse.error(401, "登录状态已过期，请重新登录");
            }
            long userId = StpUtil.getLoginIdAsLong();
            SysUser user = sysUserMapper.findById(userId);
            if (user == null) {
                return ApiResponse.error(401, "用户不存在，请重新登录");
            }
            user.setPassword(null);

            List<String> roles = sysUserMapper.findRoleCodesByUserId(userId);
            List<String> perms = userId == 1L ? List.of("*") : sysUserMapper.findPermissionCodesByUserId(userId);
            List<?> menus = userId == 1L
                    ? sysRoleMapper.findAllMenus()
                    : sysRoleMapper.findMenusByUserId(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("userInfo", user);
            result.put("roles", roles);
            result.put("permissions", perms);
            result.put("menus", menus);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(401, "登录状态已过期，请重新登录");
        }
    }

    @PostMapping("/admin/logout")
    public ApiResponse<Void> adminLogout() {
        StpUtil.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/system/admin")
    public ApiResponse<Map<String, Object>> getAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<SysUser> list = sysUserMapper.findByPage(offset, pageSize);
        list.forEach(u -> u.setPassword(null));

        // 为每个用户获取角色编码
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SysUser user : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getRealName());
            item.put("email", user.getEmail());
            item.put("phone", user.getPhone());
            item.put("status", user.getStatus() == 1 ? "active" : "disabled");
            item.put("lastLoginTime", user.getLastLoginTime());
            item.put("createTime", user.getCreatedAt());
            item.put("updateTime", user.getUpdatedAt());

            // 获取用户的第一个角色编码（主要角色）
            List<String> roleCodes = sysUserMapper.findRoleCodesByUserId(user.getId());
            String primaryRoleCode = roleCodes.isEmpty() ? "" : roleCodes.get(0);
            item.put("roleCode", primaryRoleCode);

            resultList.add(item);
        }

        int total = sysUserMapper.count();
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    /**
     * 新建管理员（默认密码 admin123 → BCrypt 加密）
     * 权限控制：
     *  - 超级管理员(SUPER_ADMIN)：可创建任意一级/二级角色
     *  - 一级管理员(LEVEL1_ADMIN)：只能创建二级角色
     *  - 二级管理员：无权限创建
     */
    @PostMapping("/system/admin")
    public ApiResponse<Map<String, Object>> createAdmin(@RequestBody Map<String, Object> body) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> currentUserRoles = sysUserMapper.findRoleCodesByUserId(currentUserId);

        // 权限检查：只有超级管理员或一级管理员能创建
        boolean isSuperAdmin = currentUserRoles.contains("SUPER_ADMIN");
        boolean isLevel1Admin = currentUserRoles.contains("LEVEL1_ADMIN");
        if (!isSuperAdmin && !isLevel1Admin) {
            return ApiResponse.error(403, "您没有创建管理员的权限");
        }

        // 获取请求参数
        String username = (String) body.get("username");
        String realName = (String) body.get("realName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String roleCode = (String) body.get("role");
        String password = (String) body.get("password");

        // 一级管理员只能创建二级角色
        if (isLevel1Admin && roleCode != null) {
            SysRole role = sysRoleMapper.findByCode(roleCode);
            if (role == null || role.getRoleLevel() == null || role.getRoleLevel() <= 2) {
                return ApiResponse.error(403, "一级管理员只能创建二级管理员角色");
            }
        }

        // 用户名唯一检查
        if (sysUserMapper.findByUsername(username) != null) {
            return ApiResponse.error("用户名已存在");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        String rawPwd = (password == null || password.isEmpty()) ? "admin123" : password;
        user.setPassword(PasswordEncoderUtil.bcryptEncode(rawPwd));
        user.setStatus(1);
        user.setAccountType(1);
        user.setRealName(realName != null ? realName : "未设置");
        user.setEmail(email);
        user.setPhone(phone);
        user.setCreatedBy(currentUserId);
        sysUserMapper.insert(user);

        // 分配角色
        if (roleCode != null && !roleCode.isEmpty()) {
            SysRole role = sysRoleMapper.findByCode(roleCode);
            if (role != null) {
                // 检查是否已存在关联
                sysUserMapper.insertUserRole(user.getId(), role.getId());
            }
        }

        // 返回结果
        SysUser created = sysUserMapper.findById(user.getId());
        if (created != null) created.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", created != null ? created.getId() : null);
        result.put("username", created != null ? created.getUsername() : null);
        result.put("roleCode", roleCode);
        return ApiResponse.success(result);
    }

    @PutMapping("/system/admin/{id}")
    public ApiResponse<SysUser> updateAdmin(@PathVariable Long id, @RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 更新密码也走 BCrypt（超级管理员密码仅允许 id=1 自己改 Argon2id，这里通用 BCrypt 覆盖）
            user.setPassword(PasswordEncoderUtil.bcryptEncode(user.getPassword()));
            sysUserMapper.updatePassword(id, user.getPassword());
        }
        user.setId(id);
        sysUserMapper.update(user);

        SysUser updated = sysUserMapper.findById(id);
        if (updated != null) updated.setPassword(null);
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/system/admin/{id}")
    public ApiResponse<Void> deleteAdmin(@PathVariable Long id) {
        if (id == 1L) {
            return ApiResponse.error("超级管理员不可删除");
        }
        boolean success = sysUserMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @PostMapping("/system/admin/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        // 超级管理员保持 Argon2id（algovize123），其他管理员重置为 BCrypt(admin123)
        String encoded;
        if (id == 1L) {
            encoded = "$argon2id$v=19$m=65536,t=3,p=1$IOLmhNf9s/Z03JV7O/tJZA$YT/C7YsDrOroO1hD5Ik2cpXkPSja6J0y8xtVdcjbq88";
        } else {
            encoded = PasswordEncoderUtil.bcryptEncode("admin123");
        }
        boolean success = sysUserMapper.updatePassword(id, encoded) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("重置密码失败");
    }

    @PutMapping("/system/admin/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status;
        Object v = body.get("status");
        if (v instanceof Number) status = ((Number) v).intValue();
        else if ("active".equals(v)) status = 1;
        else if ("disabled".equals(v)) status = 0;
        else status = Integer.valueOf(String.valueOf(v));
        boolean success = sysUserMapper.updateStatus(id, status) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("状态变更失败");
    }
}