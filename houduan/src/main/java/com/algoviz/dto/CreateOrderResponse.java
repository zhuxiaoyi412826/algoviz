package com.algoviz.dto;

public class CreateOrderResponse {
    
    private boolean success;
    private String message;
    private String orderId;
    private String qrCodeUrl;
    private String qrCodeText;
    private String productName;
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
