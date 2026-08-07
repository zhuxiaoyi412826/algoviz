package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 批量添加题目响应 DTO
 */
@Schema(description = "批量添加题目响应")
public class BatchAddProblemsResponse {

    @Schema(description = "是否成功")
    private Boolean success;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "成功数")
    private Integer successCount;
    @Schema(description = "失败数")
    private Integer failedCount;
    @Schema(description = "失败原因列表")
    private List<String> failedReasons;

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
