package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "订单实体")
public class Order {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "订单号")
    private String orderId;
    @Schema(description = "商品ID")
    private String productId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "金额")
    private int amount;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "订单状态")
    private String status;
    @Schema(description = "微信商户订单号")
    private String wechatTradeNo;
    @Schema(description = "微信交易号")
    private String wechatTransactionId;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "支付时间")
    private LocalDateTime payTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "退款状态")
    private String refundStatus;
    @Schema(description = "退款时间")
    private LocalDateTime refundTime;
    @Schema(description = "退款原因")
    private String refundReason;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "邮箱")
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