package com.algoviz.dto;

import java.util.List;

/**
 * 批量添加题目响应 DTO
 */
public class BatchAddProblemsResponse {

    private Boolean success;
    private String message;
    private Integer successCount;
    private Integer failedCount;
    private List<String> failedReasons;  // 失败题目的原因列表

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

    public List<String> getFailedReasons() { return failedReasons; }
    public void setFailedReasons(List<String> failedReasons) { this.failedReasons = failedReasons; }
}
