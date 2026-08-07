package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "提交记录实体")
public class Submission {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "提交ID")
    private String submissionId;
    @Schema(description = "题目ID")
    private Long problemId;
    @Schema(description = "题目标题")
    private String problemTitle;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "提交代码")
    private String code;
    @Schema(description = "编程语言")
    private String language;
    @Schema(description = "评测状态")
    private String status;
    @Schema(description = "运行时间(ms)")
    private Integer runtime;
    @Schema(description = "内存消耗(KB)")
    private Integer memory;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "评测日志")
    private String judgeLog;
    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
    @Schema(description = "评测时间")
    private LocalDateTime judgeTime;

    public Submission() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getJudgeLog() {
        return judgeLog;
    }

    public void setJudgeLog(String judgeLog) {
        this.judgeLog = judgeLog;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getJudgeTime() {
        return judgeTime;
    }

    public void setJudgeTime(LocalDateTime judgeTime) {
        this.judgeTime = judgeTime;
    }
}