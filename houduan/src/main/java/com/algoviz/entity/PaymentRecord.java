package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "支付记录实体")
public class PaymentRecord {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "订单号")
    private String orderId;
    @Schema(description = "商品ID")
    private String productId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "支付金额")
    private Integer amount;
    @Schema(description = "支付方式")
    private String paymentMethod;
    @Schema(description = "交易ID")
    private String transactionId;
    @Schema(description = "支付状态")
    private String status;
    @Schema(description = "退款状态")
    private String refundStatus;
    @Schema(description = "退款原因")
    private String refundReason;
    @Schema(description = "创建时间")
    private String createTime;
    @Schema(description = "支付时间")
    private String payTime;
    @Schema(description = "退款时间")
    private String refundTime;

    public PaymentRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getPayTime() { return payTime; }
    public void setPayTime(String payTime) { this.payTime = payTime; }
    public String getRefundTime() { return refundTime; }
    public void setRefundTime(String refundTime) { this.refundTime = refundTime; }
}