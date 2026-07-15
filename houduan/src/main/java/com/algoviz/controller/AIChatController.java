package com.algoviz.controller;

import com.algoviz.dto.AIGenerateProblemRequest;
import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.dto.ChatRequest;
import com.algoviz.service.AIChatService;
import com.algoviz.service.AIProblemService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI聊天", description = "AI助手相关接口")
public class AIChatController {

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private AIProblemService aiProblemService;

    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    @Operation(summary = "AI聊天", description = "与AI助手对话")
    public Map<String, Object> chat(@RequestBody ChatRequest chatRequest) {
        System.out.println("[AI Controller] 收到请求，消息数: " + (chatRequest.getMessages() != null ? chatRequest.getMessages().size() : 0));
        Map<String, Object> result = new HashMap<>();
        HttpURLConnection connection = null;
        
        try {
            System.out.println("[AI Controller] 正在调用 DeepSeek API (非流式)...");
            connection = aiChatService.chat(chatRequest, false);
            
            int responseCode = connection.getResponseCode();
            System.out.println("[AI Controller] DeepSeek API 响应码: " + responseCode);
            
            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                }
                System.out.println("[AI Controller] 响应长度: " + responseBody.length());
                
                result.put("success", true);
                result.put("data", responseBody.toString());
            } else {
                System.out.println("[AI Controller] API 错误响应码: " + responseCode);
                InputStream errorStream = connection.getErrorStream();
                String errorMsg = "";
                if (errorStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        errorMsg = sb.toString();
                        System.out.println("[AI Controller] 错误详情: " + errorMsg);
                    }
                }
                result.put("success", false);
                result.put("error", "DeepSeek API 错误: " + responseCode + " - " + errorMsg);
            }
        } catch (Exception e) {
            System.out.println("[AI Controller] 发生异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    /**
     * AI 生成题目（同步版）
     * 适合题目数量较小（≤3 道）的场景
     */
    @PostMapping("/generate-problems")
    @Operation(summary = "AI生成题目（同步）", description = "根据配置参数调用 DeepSeek 生成 OJ 题目")
    public AIGenerateProblemResponse generateProblems(@RequestBody AIGenerateProblemRequest request) {
        return aiProblemService.generateProblems(request);
    }

    /**
     * AI 生成题目（SSE 流式版）
     * 适合生成数量较多（4~10 道）的场景，前端可实时看到生成进度
     */
    @PostMapping(value = "/generate-problems/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI生成题目（流式）", description = "SSE 流式推送 AI 生成题目的进度与结果")
    public SseEmitter generateProblemsStream(@RequestBody AIGenerateProblemRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时

        SSE_EXECUTOR.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("status").data("{\"stage\":\"start\",\"message\":\"开始生成题目...\"}"));

                HttpURLConnection connection = aiProblemService.generateProblemsStream(request);
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    String err = readError(connection);
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"DeepSeek 调用失败: " + responseCode + " " + err + "\"}"));
                    emitter.complete();
                    return;
                }

                StringBuilder fullContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // DeepSeek SSE 格式: data: {...}
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                                if (!delta.isEmpty()) {
                                    fullContent.append(delta);
                                    emitter.send(SseEmitter.event().name("delta").data(delta));
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                }

                // 解析完整 JSON
                AIGenerateProblemResponse resp = aiProblemService.parseGeneratedContent(fullContent.toString(), request);
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(resp)));
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ignore) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String readError(HttpURLConnection connection) {
        StringBuilder sb = new StringBuilder();
        try (InputStream es = connection.getErrorStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
        } catch (Exception ignore) {}
        return sb.toString();
    }
}
