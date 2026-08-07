package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API日志实体")
public class ApiLog {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "API路径")
    private String apiPath;
    @Schema(description = "HTTP方法")
    private String httpMethod;
    @Schema(description = "状态码")
    private Integer statusCode;
    @Schema(description = "响应时间(ms)")
    private Long responseTime;
    @Schema(description = "客户端IP")
    private String clientIp;
    @Schema(description = "请求体")
    private String requestBody;
    @Schema(description = "响应体")
    private String responseBody;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "创建时间")
    private String createTime;

    public ApiLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}