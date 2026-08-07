package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "查询订单响应")
public class QueryOrderResponse {
    
    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "订单状态")
    private String status;
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "金额")
    private int amount;
    
    public static QueryOrderResponse success(String status, String orderId, 
                                              String productName, int amount) {
        QueryOrderResponse response = new QueryOrderResponse();
        response.success = true;
        response.status = status;
        response.orderId = orderId;
        response.productName = productName;
        response.amount = amount;
        return response;
    }
    
    public static QueryOrderResponse fail(String message) {
        QueryOrderResponse response = new QueryOrderResponse();
        response.success = false;
        response.message = message;
        return response;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
