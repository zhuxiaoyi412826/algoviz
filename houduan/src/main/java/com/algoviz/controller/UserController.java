package com.algoviz.controller;

import com.algoviz.entity.User;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据ID获取用户信息")
    public User getUser(@PathVariable Integer id) {
        // 实际项目中应该根据token获取用户ID
        return userService.findByUsername("mock_openid_123456");
    }

    @PutMapping
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public User updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }
}
