package com.algoviz.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

/**
 * AI 生成题目响应 DTO
 * 单道题的结构对应 OJProblem（不包含 id/status/submissionCount/acRate/createdAt/updatedAt 等后端自动填充字段）
 */
public class AIGenerateProblemResponse {

    /** 是否成功 */
    private Boolean success;

    /** 错误信息（失败时填充） */
    private String message;

    /** 生成的多道题目（不包含入库所需 ID） */
    private List<GeneratedProblem> problems;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<GeneratedProblem> getProblems() { return problems; }
    public void setProblems(List<GeneratedProblem> problems) { this.problems = problems; }

    /**
     * AI 生成的单道题（中间结构，供前端预览 / 二次编辑）
     */
    public static class GeneratedProblem {
        /** 草稿 ID（前端可借此标识"第几题"，批量入库时由后端重新分配 UUID） */
        private String draftId;

        /** 建议题号（4 位数字字符串，可被管理员修改） */
        private String problemNo;

        /** 标题 */
        private String title;

        /** 难度 */
        private String difficulty;

        /** 标签（英文逗号分隔字符串） */
        private String tags;

        /** 题目描述 HTML 片段 */
        private String description;

        /** 输入格式 */
        private String inputFormat;

        /** 输出格式 */
        private String outputFormat;

        /** 样例输入 */
        private String sampleInput;

        /** 样例输出 */
        private String sampleOutput;

        /** 提示（解题思路） */
        private String hint;

        /** 模板代码（根据 language 字段生成对应语言的 class Solution 框架） */
        private String template;

        /** 生成使用的语言（java/python/cpp/javascript/general） */
        private String language;

        public String getDraftId() { return draftId; }
        public void setDraftId(String draftId) { this.draftId = draftId; }

        public String getProblemNo() { return problemNo; }
        public void setProblemNo(String problemNo) { this.problemNo = problemNo; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public String getTags() { return tags; }

        @JsonSetter("tags")
        public void setTags(Object tags) {
            if (tags == null) {
                this.tags = null;
            } else if (tags instanceof String) {
                this.tags = (String) tags;
            } else if (tags instanceof List) {
                List<?> list = (List<?>) tags;
                StringBuilder sb = new StringBuilder();
                for (Object item : list) {
                    String s = item == null ? "" : item.toString().trim();
                    if (!s.isEmpty()) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(s);
                    }
                }
                this.tags = sb.toString();
            } else {
                this.tags = tags.toString();
            }
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getInputFormat() { return inputFormat; }
        public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

        public String getSampleInput() { return sampleInput; }
        public void setSampleInput(String sampleInput) { this.sampleInput = sampleInput; }

        public String getSampleOutput() { return sampleOutput; }
        public void setSampleOutput(String sampleOutput) { this.sampleOutput = sampleOutput; }

        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }

        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}
