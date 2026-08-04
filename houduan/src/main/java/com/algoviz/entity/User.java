package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private Integer age;
    private String gender;
    private String nickname;
    private String avatarUrl;
    private String loginStatus;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
