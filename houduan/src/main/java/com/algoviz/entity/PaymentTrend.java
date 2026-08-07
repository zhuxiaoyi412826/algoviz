package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "支付趋势实体")
public class PaymentTrend {
    @Schema(description = "日期")
    private String date;
    @Schema(description = "支付金额")
    private Integer amount;

    public PaymentTrend() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}