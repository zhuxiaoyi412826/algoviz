package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建订单请求")
public class CreateOrderRequest {
    
    @Schema(description = "商品ID")
    private String productId;
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
}
