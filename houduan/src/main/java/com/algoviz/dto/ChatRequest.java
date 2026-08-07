package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "聊天请求")
public class ChatRequest {
    @Schema(description = "消息列表")
    private List<Message> messages;

    public ChatRequest() {}

    public ChatRequest(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Schema(description = "单条消息")
    public static class Message {
        @Schema(description = "角色")
        private String role;
        @Schema(description = "内容")
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
