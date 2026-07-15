package com.algoviz.entity;

import java.time.LocalDateTime;

public class Submission {
    
    private Long id;
    private String submissionId;
    private Long problemId;
    private String problemTitle;
    private Long userId;
    private String username;
    private String code;
    private String language;
    private String status;
    private Integer runtime;
    private Integer memory;
    private String errorMessage;
    private String judgeLog;
    private LocalDateTime submitTime;
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