package com.algoviz.service;

import com.algoviz.dto.AIGenerateProblemRequest;
import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.dto.ChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 生成题目服务
 * 负责：Prompt 构造 → 调用 DeepSeek → 解析 JSON 结果 → 构造 GeneratedProblem 列表
 */
@Service
public class AIProblemService {

    private static final Logger logger = LoggerFactory.getLogger(AIProblemService.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-v4-pro";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 题号生成：注入 OJProblemService 查数据库最大题号 */
    @Autowired
    private OJProblemService ojProblemService;

    /**
     * 同步生成题目（一次性返回完整结果）
     */
    public AIGenerateProblemResponse generateProblems(AIGenerateProblemRequest request) {
        logger.info("开始 AI 生成题目: difficulty={}, count={}, language={}, style={}",
                request.getDifficulty(), request.getCount(), request.getLanguage(), request.getStyle());

        AIGenerateProblemResponse response = new AIGenerateProblemResponse();

        try {
            // 1. 参数校验
            if (request.getCount() == null || request.getCount() < 1) {
                request.setCount(1);
            }
            if (request.getCount() > 10) {
                request.setCount(10);
            }

            // 2. 构造 Prompt
            String systemPrompt = AIGenerateProblemPrompt.SYSTEM_PROMPT;
            String userPrompt = buildUserPrompt(request);
            logger.debug("User Prompt: {}", userPrompt);

            // 3. 调用 DeepSeek（同步模式）
            HttpURLConnection connection = callDeepSeek(systemPrompt, userPrompt, false);

            // 4. 读取响应
            String rawResponse = readResponse(connection);
            logger.debug("DeepSeek 原始返回: {}", rawResponse);

            // 5. 解析 choices[0].message.content
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            logger.info("DeepSeek content 长度: {}", content.length());

            // 6. 解析 content（可能包 ```json 包装 + 末尾可能有多余内容）
            String jsonContent = stripMarkdownCodeBlock(content);
            // === 关键修复：多道题时 AI 经常在示例里插入未转义的换行/控制字符，破坏 JSON ===
            // 策略：找到最后一个 "}]" 或 "}" 作为 JSON 截断点
            jsonContent = sanitizeAIJson(jsonContent);
            logger.debug("清洗后 JSON: {}", jsonContent);
            JsonNode problemsNode;
            try {
                problemsNode = objectMapper.readTree(jsonContent).path("problems");
            } catch (Exception parseEx) {
                logger.error("JSON 解析失败，原始 content: {}", content);
                throw new RuntimeException("AI 返回内容非合法 JSON（多道题时可能格式异常）。请重试或减少到 1 道。原始错误：" + parseEx.getMessage());
            }

            // 7. 构造 GeneratedProblem 列表
            List<AIGenerateProblemResponse.GeneratedProblem> problems = new ArrayList<>();
            if (problemsNode.isArray()) {
                // === 关键：一次性查数据库最大题号，顺序分配，避免 AI 写死 2001/3001 ===
                int problemCount = problemsNode.size();
                List<String> preAllocatedNos = ojProblemService.generateNextProblemNos(problemCount);
                logger.info("AI 预分配题号: {}", preAllocatedNos);

                int index = 0;
                for (JsonNode p : problemsNode) {
                    AIGenerateProblemResponse.GeneratedProblem gp = new AIGenerateProblemResponse.GeneratedProblem();
                    gp.setDraftId(UUID.randomUUID().toString().substring(0, 8));
                    // 后端统一分配真实题号（数据库 max + 1, +2, ...）
                    gp.setProblemNo(index < preAllocatedNos.size()
                            ? preAllocatedNos.get(index)
                            : ojProblemService.generateNextProblemNo());
                    gp.setTitle(p.path("title").asText("未命名题目"));
                    gp.setDifficulty(p.path("difficulty").asText(request.getDifficulty()));
                    gp.setTags(p.path("tags").asText(""));
                    gp.setDescription(p.path("description").asText(""));
                    gp.setInputFormat(p.path("inputFormat").asText(""));
                    gp.setOutputFormat(p.path("outputFormat").asText(""));
                    gp.setSampleInput(p.path("sampleInput").asText(""));
                    gp.setSampleOutput(p.path("sampleOutput").asText(""));
                    gp.setHint(p.path("hint").asText(""));
                    String template = p.path("template").asText("");
                    if (template.isEmpty()) {
                        template = AIGenerateProblemPrompt.defaultTemplate(request.getLanguage());
                    }
                    gp.setTemplate(template);
                    gp.setLanguage(request.getLanguage());
                    problems.add(gp);
                    index++;
                }
            }

            response.setSuccess(true);
            response.setMessage("成功生成 " + problems.size() + " 道题目");
            response.setProblems(problems);

        } catch (Exception e) {
            logger.error("AI 生成题目失败", e);
            response.setSuccess(false);
            response.setMessage("AI 生成失败：" + e.getMessage());
            response.setProblems(new ArrayList<>());
        }

        return response;
    }

    /**
     * SSE 流式生成 - 返回 DeepSeek 的 HttpURLConnection，前端可直接读取流
     */
    public HttpURLConnection generateProblemsStream(AIGenerateProblemRequest request) throws Exception {
        if (request.getCount() == null || request.getCount() < 1) {
            request.setCount(1);
        }
        if (request.getCount() > 10) {
            request.setCount(10);
        }

        String systemPrompt = AIGenerateProblemPrompt.SYSTEM_PROMPT;
        String userPrompt = buildUserPrompt(request);
        return callDeepSeek(systemPrompt, userPrompt, true);
    }

    /**
     * 构造用户提示词
     */
    private String buildUserPrompt(AIGenerateProblemRequest request) {
        String knowledgePoints;
        if (request.getKnowledgePoints() == null || request.getKnowledgePoints().isEmpty()) {
            knowledgePoints = "不限（由你按难度自由选择合适的知识点）";
        } else {
            knowledgePoints = String.join("、", request.getKnowledgePoints());
        }

        String language = AIGenerateProblemPrompt.getLanguageDescription(request.getLanguage());
        String style = AIGenerateProblemPrompt.getStyleDescription(request.getStyle());
        String difficulty = AIGenerateProblemPrompt.getDifficultyDescription(request.getDifficulty());

        String additionalSection = "";
        if (request.getAdditionalRequirements() != null && !request.getAdditionalRequirements().trim().isEmpty()) {
            additionalSection = "【额外要求】\n" + request.getAdditionalRequirements().trim() + "\n";
        }

        return AIGenerateProblemPrompt.USER_PROMPT_TEMPLATE
                .replace("{count}", String.valueOf(request.getCount()))
                .replace("{difficulty}", difficulty)
                .replace("{knowledgePoints}", knowledgePoints)
                .replace("{language}", language)
                .replace("{style}", style)
                .replace("{additionalRequirementsSection}", additionalSection);
    }

    /**
     * 清洗 AI 返回的 JSON：多道题时 AI 经常在 description/sample 里塞未转义的换行/制表符，
     * 导致 Jackson 抛 "Illegal unquoted character" / "has to be escaped" 错误。
     *
     * 处理策略：
     *  1. 找到第一个 "{" 和最后一个 "}"，裁掉前后多余文字
     *  2. 把所有 ASCII 控制字符（0x00~0x1F）替换为空格
     */
    private String sanitizeAIJson(String raw) {
        if (raw == null) return "{}";
        String s = raw;

        // 1. 裁剪前后
        int firstBrace = s.indexOf('{');
        int lastBrace = s.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            s = s.substring(firstBrace, lastBrace + 1);
        }

        // 2. 替换非法控制字符（包括裸换行）为空格
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 调用 DeepSeek
     */
    private HttpURLConnection callDeepSeek(String systemPrompt, String userPrompt, boolean stream) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);

        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatRequest.Message("system", systemPrompt));
        messages.add(new ChatRequest.Message("user", userPrompt));

        String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":%s,\"stream\":%s,\"temperature\":0.7,\"response_format\":{\"type\":\"json_object\"}}",
                MODEL,
                toJson(messages),
                stream
        );

        try (java.io.OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 去掉 AI 返回的 Markdown 代码块标记
     */
    private String stripMarkdownCodeBlock(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * 当 AI 未返回 problemNo 时，给出兜底建议（4 位数字）
     */
    private String suggestProblemNo(String difficulty, int index) {
        String prefix;
        if (difficulty == null) {
            prefix = "2";
        } else {
            switch (difficulty.toLowerCase()) {
                case "easy":   prefix = "1"; break;
                case "hard":   prefix = "3"; break;
                case "medium":
                default:       prefix = "2"; break;
            }
        }
        int n = (int) (System.currentTimeMillis() % 9000) + 1000;
        return prefix + String.format("%03d", n % 1000);
    }

    /**
     * 把 AI 返回的原始 content 解析成 GeneratedProblem 列表（供 SSE 完成后调用）
     */
    public AIGenerateProblemResponse parseGeneratedContent(String rawContent, AIGenerateProblemRequest request) {
        AIGenerateProblemResponse response = new AIGenerateProblemResponse();
        try {
            String jsonContent = stripMarkdownCodeBlock(rawContent);
            JsonNode problemsNode = objectMapper.readTree(jsonContent).path("problems");

            List<AIGenerateProblemResponse.GeneratedProblem> problems = new ArrayList<>();
            if (problemsNode.isArray()) {
                // === 关键：流式版同样批量查数据库最大题号，顺序分配 ===
                int problemCount = problemsNode.size();
                List<String> preAllocatedNos = ojProblemService.generateNextProblemNos(problemCount);
                logger.info("[流式]AI 预分配题号: {}", preAllocatedNos);

                int index = 0;
                for (JsonNode p : problemsNode) {
                    AIGenerateProblemResponse.GeneratedProblem gp = new AIGenerateProblemResponse.GeneratedProblem();
                    gp.setDraftId(UUID.randomUUID().toString().substring(0, 8));
                    // 后端统一分配真实题号（数据库 max + 1, +2, ...）
                    gp.setProblemNo(index < preAllocatedNos.size()
                            ? preAllocatedNos.get(index)
                            : ojProblemService.generateNextProblemNo());
                    gp.setTitle(p.path("title").asText("未命名题目"));
                    gp.setDifficulty(p.path("difficulty").asText(request.getDifficulty()));
                    gp.setTags(p.path("tags").asText(""));
                    gp.setDescription(p.path("description").asText(""));
                    gp.setInputFormat(p.path("inputFormat").asText(""));
                    gp.setOutputFormat(p.path("outputFormat").asText(""));
                    gp.setSampleInput(p.path("sampleInput").asText(""));
                    gp.setSampleOutput(p.path("sampleOutput").asText(""));
                    gp.setHint(p.path("hint").asText(""));
                    String template = p.path("template").asText("");
                    if (template.isEmpty()) {
                        template = AIGenerateProblemPrompt.defaultTemplate(request.getLanguage());
                    }
                    gp.setTemplate(template);
                    gp.setLanguage(request.getLanguage());
                    problems.add(gp);
                    index++;
                }
            }
            response.setSuccess(true);
            response.setMessage("成功生成 " + problems.size() + " 道题目");
            response.setProblems(problems);
        } catch (Exception e) {
            logger.error("解析 AI content 失败", e);
            response.setSuccess(false);
            response.setMessage("解析失败：" + e.getMessage());
            response.setProblems(new ArrayList<>());
        }
        return response;
    }

    private String toJson(List<ChatRequest.Message> messages) {
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
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
