package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建订单响应")
public class CreateOrderResponse {
    
    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "二维码URL")
    private String qrCodeUrl;
    @Schema(description = "二维码文本")
    private String qrCodeText;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "金额")
    private int amount;
    
    public static CreateOrderResponse success(String orderId, String qrCodeText, 
                                               String productName, int amount) {
        CreateOrderResponse response = new CreateOrderResponse();
        response.success = true;
        response.orderId = orderId;
        response.qrCodeText = qrCodeText;
        response.productName = productName;
        response.amount = amount;
        return response;
    }
    
    public static CreateOrderResponse fail(String message) {
        CreateOrderResponse response = new CreateOrderResponse();
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
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
    
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
    
    public String getQrCodeText() {
        return qrCodeText;
    }
    
    public void setQrCodeText(String qrCodeText) {
        this.qrCodeText = qrCodeText;
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
