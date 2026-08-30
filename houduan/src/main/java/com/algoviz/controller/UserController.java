package com.algoviz.controller;

import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口", description = "前端用户接口")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据ID获取用户信息")
    public User getUser(@PathVariable Integer id) {
        // 实际项目中应该根据token获取用户ID
        User user = userService.findByUsername("mock_openid_123456");
        if (user != null) {
            user.setPassword(null);   // 脱敏：不返回密码哈希
        }
        return user;
    }

    @PutMapping
    @Operation(summary = "更新用户信息", description = "更新用户信息（密码不允许通过此接口修改，请走 /api/login/change-password）")
    public User updateUser(@RequestBody User user) {
        // 防止前端通过此接口篡改密码（改密必须走专门接口 + 旧密码校验）
        user.setPassword(null);
        User updated = userService.updateUser(user);
        if (updated != null) {
            updated.setPassword(null);   // 脱敏：不返回密码哈希
        }
        return updated;
    }
}
