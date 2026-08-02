package com.algoviz.service;

import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.dto.BatchAddProblemsResponse;
import com.algoviz.entity.OJProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Markdown 题目导入服务
 *
 * MD 模板格式：
 *   # 题号: 1500
 *   # 标题: 设计一个有getMin功能的栈
 *   # 难度: easy
 *   # 标签: 栈,数据结构
 *   # 题目描述:
 *   实现一个特殊的栈...
 *   # 代码模板:
 *   public class Solution {}
 *   ---  (分隔符，可选)
 *   # 题号: 1501
 *   ...
 */
@Service
public class MdImportService {

    private static final Logger logger = LoggerFactory.getLogger(MdImportService.class);

    @Autowired
    private OJProblemService problemService;

    public BatchAddProblemsResponse importFromMd(MultipartFile file, boolean overwriteOnConflict) {
        BatchAddProblemsResponse response = new BatchAddProblemsResponse();
        List<String> failedReasons = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        if (file == null || file.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("文件为空");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".md") && !filename.endsWith(".markdown")) {
            response.setSuccess(false);
            response.setMessage("仅支持 .md / .markdown 格式");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        List<AIGenerateProblemResponse.GeneratedProblem> problems = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            List<Map<String, String>> parsedProblems = parseMd(lines);
            logger.info("MD 解析完成：{} 道题目", parsedProblems.size());

            for (Map<String, String> fields : parsedProblems) {
                try {
                    AIGenerateProblemResponse.GeneratedProblem gp = new AIGenerateProblemResponse.GeneratedProblem();
                    gp.setProblemNo(getField(fields, "题号", "problemNo"));
                    gp.setTitle(getField(fields, "标题", "title"));
                    gp.setDifficulty(normalizeDifficulty(getField(fields, "难度", "difficulty", "medium")));
                    gp.setTags(getField(fields, "标签", "tags"));
                    gp.setDescription(getField(fields, "题目描述", "description"));
                    gp.setInputFormat(getField(fields, "输入格式", "inputFormat"));
                    gp.setOutputFormat(getField(fields, "输出格式", "outputFormat"));
                    gp.setSampleInput(getField(fields, "样例输入", "sampleInput"));
                    gp.setSampleOutput(getField(fields, "样例输出", "sampleOutput"));
                    gp.setHint(getField(fields, "解题提示", "hint"));
                    gp.setTemplate(getField(fields, "代码模板", "template"));
                    problems.add(gp);
                } catch (Exception ex) {
                    failedCount++;
                    failedReasons.add("MD 解析失败：" + ex.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("MD 解析异常", e);
            response.setSuccess(false);
            response.setMessage("MD 解析失败：" + e.getMessage());
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        if (problems.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("MD 中无有效题目数据");
            response.setSuccessCount(0);
            response.setFailedCount(failedCount);
            response.setFailedReasons(failedReasons);
            return response;
        }

        for (int i = 0; i < problems.size(); i++) {
            AIGenerateProblemResponse.GeneratedProblem gp = problems.get(i);
            try {
                if (gp.getTitle() == null || gp.getTitle().trim().isEmpty()) {
                    failedCount++;
                    failedReasons.add("第 " + (i + 1) + " 道：标题为空");
                    continue;
                }

                if (gp.getProblemNo() == null || gp.getProblemNo().trim().isEmpty()) {
                    gp.setProblemNo(problemService.generateNextProblemNo());
                }

                String diff = gp.getDifficulty();
                if (diff == null || (!diff.equals("easy") && !diff.equals("medium") && !diff.equals("hard"))) {
                    gp.setDifficulty("medium");
                }

                String fullDescription = buildDescription(gp);
                String fullTemplate = gp.getTemplate() == null ? "" : gp.getTemplate();

                OJProblem entity = new OJProblem();
                entity.setProblemNo(gp.getProblemNo());
                entity.setTitle(gp.getTitle());
                entity.setDifficulty(gp.getDifficulty());
                entity.setTags(gp.getTags() == null ? "" : gp.getTags());
                entity.setDescription(fullDescription);
                entity.setTemplate(fullTemplate);
                entity.setStatus("ACTIVE");
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                entity.setSubmissionCount(0);

                OJProblem existing = problemService.getProblemByNo(gp.getProblemNo());
                if (existing != null) {
                    if (overwriteOnConflict) {
                        entity.setId(existing.getId());
                        problemService.updateProblem(entity);
                        logger.info("第 {} 道：题号 {} 覆盖更新", i + 1, gp.getProblemNo());
                    } else {
                        String newNo = problemService.generateNextProblemNo();
                        entity.setProblemNo(newNo);
                        problemService.addProblem(entity);
                        logger.info("第 {} 道：题号冲突，重新分配 {}", i + 1, newNo);
                    }
                } else {
                    problemService.addProblem(entity);
                }
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failedReasons.add("第 " + (i + 1) + " 道「" + (gp.getTitle() == null ? "无标题" : gp.getTitle()) + "」入库失败：" + e.getMessage());
                logger.error("入库失败", e);
            }
        }

        response.setSuccess(failedCount == 0);
        response.setMessage(String.format("导入完成：成功 %d 道，失败 %d 道", successCount, failedCount));
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setFailedReasons(failedReasons);
        return response;
    }

    /**
     * 解析 MD 文件：按 --- 分隔符拆分为多道题，每道题内按 # 字段名: 提取内容
     */
    private List<Map<String, String>> parseMd(List<String> lines) {
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, String> current = null;
        String currentField = null;
        StringBuilder fieldContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine;

            // 检测分隔符
            if (line.trim().equals("---")) {
                if (current != null && !current.isEmpty()) {
                    if (currentField != null) {
                        current.put(currentField, fieldContent.toString().trim());
                    }
                    result.add(current);
                }
                current = null;
                currentField = null;
                fieldContent = new StringBuilder();
                continue;
            }

            // 检测字段标记：# 字段名: 或 # 字段名：
            if (line.startsWith("# ")) {
                // 保存上一个字段
                if (current != null && currentField != null) {
                    current.put(currentField, fieldContent.toString().trim());
                }

                String rest = line.substring(2).trim();
                int colonIdx = rest.indexOf(':');
                int cnColonIdx = rest.indexOf('：');
                int idx = colonIdx >= 0 && (cnColonIdx < 0 || colonIdx < cnColonIdx) ? colonIdx : cnColonIdx;

                if (idx > 0) {
                    String fieldName = rest.substring(0, idx).trim();
                    String fieldValue = rest.substring(idx + 1).trim();

                    // 如果是新题的第一个字段（题号 或 标题），创建新的题目 map
                    if (current == null) {
                        current = new LinkedHashMap<>();
                    }

                    currentField = fieldName;
                    if (!fieldValue.isEmpty()) {
                        fieldContent = new StringBuilder(fieldValue);
                    } else {
                        fieldContent = new StringBuilder();
                    }
                } else {
                    // 没有冒号的 # 行视为普通内容
                    if (current != null && currentField != null) {
                        if (fieldContent.length() > 0) fieldContent.append("\n");
                        fieldContent.append(line);
                    }
                }
            } else {
                // 普通内容行，追加到当前字段
                if (current != null && currentField != null) {
                    if (fieldContent.length() > 0) fieldContent.append("\n");
                    fieldContent.append(line);
                }
            }
        }

        // 保存最后一道题
        if (current != null && !current.isEmpty()) {
            if (currentField != null) {
                current.put(currentField, fieldContent.toString().trim());
            }
            result.add(current);
        }

        return result;
    }

    private String getField(Map<String, String> fields, String cn, String en) {
        return getField(fields, cn, en, "");
    }

    private String getField(Map<String, String> fields, String cn, String en, String defaultVal) {
        String val = fields.getOrDefault(cn, fields.get(en));
        if (val == null || val.trim().isEmpty()) return defaultVal;
        return val.trim();
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) return "medium";
        String d = difficulty.trim().toLowerCase();
        if (d.contains("简") || d.contains("eas") || d.contains("1") || d.equals("士")) return "easy";
        if (d.contains("困") || d.contains("hard") || d.contains("3") || d.equals("将")) return "hard";
        if (d.contains("中") || d.contains("medi") || d.contains("2") || d.equals("尉")) return "medium";
        return "medium";
    }

    private String buildDescription(AIGenerateProblemResponse.GeneratedProblem gp) {
        StringBuilder sb = new StringBuilder();
        if (gp.getDescription() != null && !gp.getDescription().isEmpty()) {
            sb.append(gp.getDescription());
        }
        if (gp.getInputFormat() != null && !gp.getInputFormat().isEmpty()) {
            sb.append("\n\n**输入格式**\n\n").append(gp.getInputFormat());
        }
        if (gp.getOutputFormat() != null && !gp.getOutputFormat().isEmpty()) {
            sb.append("\n\n**输出格式**\n\n").append(gp.getOutputFormat());
        }
        if (gp.getSampleInput() != null && !gp.getSampleInput().isEmpty()) {
            sb.append("\n\n**样例输入**\n\n```\n").append(gp.getSampleInput()).append("\n```");
        }
        if (gp.getSampleOutput() != null && !gp.getSampleOutput().isEmpty()) {
            sb.append("\n\n**样例输出**\n\n```\n").append(gp.getSampleOutput()).append("\n```");
        }
        if (gp.getHint() != null && !gp.getHint().isEmpty()) {
            sb.append("\n\n**解题提示**\n\n").append(gp.getHint());
        }
        return sb.toString();
    }
}
