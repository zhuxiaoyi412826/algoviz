package com.algoviz.service;

import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.dto.BatchAddProblemsResponse;
import com.algoviz.entity.OJProblem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JSON 题目导入服务
 *
 * JSON 格式：数组，每个元素为一个题目对象
 *   [
 *     {
 *       "problemNo": "1500",
 *       "title": "设计一个有getMin功能的栈",
 *       "difficulty": "easy",
 *       "tags": "栈,数据结构",
 *       "description": "题目描述",
 *       "inputFormat": "输入格式",
 *       "outputFormat": "输出格式",
 *       "sampleInput": "样例输入",
 *       "sampleOutput": "样例输出",
 *       "hint": "解题提示",
 *       "template": "代码模板"
 *     }
 *   ]
 */
@Service
public class JsonImportService {

    private static final Logger logger = LoggerFactory.getLogger(JsonImportService.class);

    @Autowired
    private OJProblemService problemService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BatchAddProblemsResponse importFromJson(MultipartFile file, boolean overwriteOnConflict) {
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
        if (!filename.endsWith(".json")) {
            response.setSuccess(false);
            response.setMessage("仅支持 .json 格式");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        List<AIGenerateProblemResponse.GeneratedProblem> problems = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(jsonText);

            // 支持两种格式：纯数组 或 带 "problems" 包装的对象
            JsonNode arrayNode;
            if (root.isArray()) {
                arrayNode = root;
            } else if (root.isObject() && root.has("problems")) {
                arrayNode = root.get("problems");
            } else {
                response.setSuccess(false);
                response.setMessage("JSON 格式错误：应为数组或包含 problems 字段的对象");
                response.setSuccessCount(0);
                response.setFailedCount(0);
                response.setFailedReasons(failedReasons);
                return response;
            }

            if (!arrayNode.isArray()) {
                response.setSuccess(false);
                response.setMessage("JSON 格式错误：problems 字段应为数组");
                return response;
            }

            int index = 0;
            for (JsonNode node : arrayNode) {
                index++;
                try {
                    AIGenerateProblemResponse.GeneratedProblem gp;
                    // 检测是否为新模板格式（含 problem.description 嵌套结构）
                    if (node.has("problem") && node.get("problem").isObject()) {
                        gp = parseTemplateFormat(node);
                    } else {
                        // 旧平铺格式
                        gp = new AIGenerateProblemResponse.GeneratedProblem();
                        gp.setProblemNo(getText(node, "problemNo", "题号"));
                        gp.setTitle(getText(node, "title", "标题"));
                        gp.setDifficulty(normalizeDifficulty(getText(node, "difficulty", "难度", "medium")));
                        gp.setTags(getText(node, "tags", "标签"));
                        gp.setDescription(getText(node, "description", "题目描述"));
                        gp.setInputFormat(getText(node, "inputFormat", "输入格式"));
                        gp.setOutputFormat(getText(node, "outputFormat", "输出格式"));
                        gp.setSampleInput(getText(node, "sampleInput", "样例输入"));
                        gp.setSampleOutput(getText(node, "sampleOutput", "样例输出"));
                        gp.setHint(getText(node, "hint", "解题提示"));
                        gp.setTemplate(getText(node, "template", "代码模板"));
                    }
                    problems.add(gp);
                } catch (Exception ex) {
                    failedCount++;
                    failedReasons.add("第 " + index + " 道解析失败：" + ex.getMessage());
                }
            }

            logger.info("JSON 解析完成：{} 道题目", problems.size());

        } catch (Exception e) {
            logger.error("JSON 解析异常", e);
            response.setSuccess(false);
            response.setMessage("JSON 解析失败：" + e.getMessage());
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        if (problems.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("JSON 中无有效题目数据");
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

    private String getText(JsonNode node, String englishKey, String chineseKey) {
        return getText(node, englishKey, chineseKey, "");
    }

    private String getText(JsonNode node, String englishKey, String chineseKey, String defaultVal) {
        JsonNode val = node.get(englishKey);
        if (val == null || val.isNull()) val = node.get(chineseKey);
        if (val == null || val.isNull()) return defaultVal;
        String s = val.asText("").trim();
        return s.isEmpty() ? defaultVal : s;
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) return "medium";
        String d = difficulty.trim().toLowerCase();
        if (d.contains("简") || d.contains("eas") || d.contains("1") || d.equals("士")) return "easy";
        if (d.contains("困") || d.contains("hard") || d.contains("3") || d.equals("将")) return "hard";
        if (d.contains("中") || d.contains("medi") || d.contains("2") || d.equals("尉")) return "medium";
        return "medium";
    }

    /**
     * 解析新模板格式（json-template.md 定义的嵌套结构）
     *
     * 映射关系：
     *   id             → problemNo
     *   title          → title
     *   difficulty     → difficulty (简单→easy, 中等→medium, 困难→hard)
     *   category       → tags (合并 solutions[].tags)
     *   problem.description → description
     *   solutions[].code    → template (所有解法代码合并)
     */
    private AIGenerateProblemResponse.GeneratedProblem parseTemplateFormat(JsonNode node) {
        AIGenerateProblemResponse.GeneratedProblem gp = new AIGenerateProblemResponse.GeneratedProblem();

        // id → problemNo
        JsonNode idNode = node.get("id");
        if (idNode != null && !idNode.isNull()) {
            gp.setProblemNo(String.valueOf(idNode.asInt()));
        }

        // title
        gp.setTitle(getText(node, "title", "标题"));

        // difficulty: 简单→easy, 中等→medium, 困难→hard
        gp.setDifficulty(normalizeDifficulty(getText(node, "difficulty", "难度", "medium")));

        // tags: category + solutions[].tags 合并
        Set<String> tagSet = new LinkedHashSet<>();
        String category = getText(node, "category", "分类", "");
        if (!category.isEmpty()) {
            tagSet.add(category);
        }
        JsonNode solutionsNode = node.get("solutions");
        if (solutionsNode != null && solutionsNode.isArray()) {
            for (JsonNode sol : solutionsNode) {
                JsonNode solTags = sol.get("tags");
                if (solTags != null && solTags.isArray()) {
                    for (JsonNode t : solTags) {
                        String tag = t.asText("").trim();
                        if (!tag.isEmpty()) tagSet.add(tag);
                    }
                }
            }
        }
        gp.setTags(String.join(",", tagSet));

        // description: problem.description + 输入输出格式 + 约束 + 示例
        JsonNode problemNode = node.get("problem");
        StringBuilder desc = new StringBuilder();
        if (problemNode != null && problemNode.isObject()) {
            String descText = getText(problemNode, "description", "题目描述", "");
            if (!descText.isEmpty()) desc.append(descText);

            String inputFmt = getText(problemNode, "inputFormat", "输入格式", "");
            if (!inputFmt.isEmpty()) desc.append("\n\n**输入格式**\n\n").append(inputFmt);

            String outputFmt = getText(problemNode, "outputFormat", "输出格式", "");
            if (!outputFmt.isEmpty()) desc.append("\n\n**输出格式**\n\n").append(outputFmt);

            JsonNode constraints = problemNode.get("constraints");
            if (constraints != null && constraints.isArray() && constraints.size() > 0) {
                desc.append("\n\n**数据范围**\n");
                for (JsonNode c : constraints) {
                    desc.append("- ").append(c.asText("")).append("\n");
                }
            }

            JsonNode examples = problemNode.get("examples");
            if (examples != null && examples.isArray()) {
                for (JsonNode ex : examples) {
                    String input = ex.has("input") ? ex.get("input").asText("") : "";
                    String output = ex.has("output") ? ex.get("output").asText("") : "";
                    String explanation = ex.has("explanation") ? ex.get("explanation").asText("") : "";
                    desc.append("\n**示例**\n");
                    desc.append("- 输入：`").append(input).append("`\n");
                    desc.append("- 输出：`").append(output).append("`\n");
                    if (!explanation.isEmpty()) {
                        desc.append("- 解释：").append(explanation).append("\n");
                    }
                }
            }
        }
        gp.setDescription(desc.toString());
        gp.setInputFormat("");
        gp.setOutputFormat("");
        gp.setSampleInput("");
        gp.setSampleOutput("");
        gp.setHint("");

        // template: 所有解法代码合并
        StringBuilder template = new StringBuilder();
        if (solutionsNode != null && solutionsNode.isArray()) {
            for (int i = 0; i < solutionsNode.size(); i++) {
                JsonNode sol = solutionsNode.get(i);
                String name = sol.has("name") ? sol.get("name").asText("") : "解法" + (i + 1);
                String approach = sol.has("approach") ? sol.get("approach").asText("") : "";
                String code = sol.has("code") ? sol.get("code").asText("") : "";
                String time = sol.has("timeComplexity") ? sol.get("timeComplexity").asText("") : "";
                String space = sol.has("spaceComplexity") ? sol.get("spaceComplexity").asText("") : "";

                template.append("// ===== 解法").append(i + 1).append(": ").append(name).append(" =====\n");
                if (!approach.isEmpty()) {
                    template.append("// 思路: ").append(approach).append("\n");
                }
                if (!time.isEmpty() || !space.isEmpty()) {
                    template.append("// 时间复杂度: ").append(time).append("  空间复杂度: ").append(space).append("\n");
                }
                template.append(code);
                if (!code.endsWith("\n")) template.append("\n");
                template.append("\n");
            }
        }
        gp.setTemplate(template.toString());

        return gp;
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
