package com.algoviz.entity;

import java.time.LocalDateTime;

public class Order {
    
    private Long id;
    private String orderId;
    private String productId;
    private String productName;
    private int amount;
    private Long userId;
    private String status;
    private String wechatTradeNo;
    private String wechatTransactionId;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime updateTime;
    private String refundStatus;
    private LocalDateTime refundTime;
    private String refundReason;
    private String username;
    private String email;
    
    public Order() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getWechatTradeNo() {
        return wechatTradeNo;
    }
    
    public void setWechatTradeNo(String wechatTradeNo) {
        this.wechatTradeNo = wechatTradeNo;
    }
    
    public String getWechatTransactionId() {
        return wechatTransactionId;
    }
    
    public void setWechatTransactionId(String wechatTransactionId) {
        this.wechatTransactionId = wechatTransactionId;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getPayTime() {
        return payTime;
    }
    
    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getRefundStatus() {
        return refundStatus;
    }
    
    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }
    
    public LocalDateTime getRefundTime() {
        return refundTime;
    }
    
    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }
    
    public String getRefundReason() {
        return refundReason;
    }
    
    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}
