package com.algoviz.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private UserInfo userInfo;
    private String token;

    @Data
    public static class UserInfo {
        private Integer id;
        private String username;
        private String email;
        private String avatar;
        private String nickname;
    }
}
