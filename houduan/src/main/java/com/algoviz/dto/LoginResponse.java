package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登录响应")
@Data
public class LoginResponse {
    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "消息提示")
    private String message;
    @Schema(description = "用户信息")
    private UserInfo userInfo;
    @Schema(description = "登录令牌")
    private String token;
    @Schema(description = "登录成功后跳转的个人界面路径")
    private String redirectUrl;

    @Schema(description = "用户信息")
    @Data
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Integer id;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "邮箱")
        private String email;
        @Schema(description = "年龄")
        private Integer age;
        @Schema(description = "昵称")
        private String nickname;
    }
}
