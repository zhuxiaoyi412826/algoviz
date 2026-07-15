package com.algoviz.service;

import com.algoviz.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class AIChatService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-chat";

    public HttpURLConnection chat(ChatRequest chatRequest, boolean stream) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        String requestBody = String.format(
            "{\"model\":\"%s\",\"messages\":%s,\"stream\":%s,\"temperature\":0.7}",
            MODEL,
            toJson(chatRequest.getMessages()),
            stream
        );
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        
        return connection;
    }
    
    private String toJson(java.util.List<ChatRequest.Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < messages.size(); i++) {
            ChatRequest.Message msg = messages.get(i);
            sb.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}", 
                escapeJson(msg.getRole()), escapeJson(msg.getContent())));
            if (i < messages.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
