package com.algoviz.service;

import com.algoviz.dto.CreateOrderResponse;
import com.algoviz.dto.QueryOrderResponse;
import com.algoviz.entity.Product;

import java.util.List;

public interface PaymentService {
    
    /**
     * 获取商品列表
     */
    List<Product> getProductList();
    
    /**
     * 获取商品信息
     */
    Product getProduct(String productId);
    
    /**
     * 创建支付订单
     */
    CreateOrderResponse createOrder(String productId, Long userId);
    
    /**
     * 查询订单状态
     */
    QueryOrderResponse queryOrder(String orderId);
    
    /**
     * 处理支付回调
     */
    void handlePaymentCallback(String jsonData);
    
    /**
     * 模拟支付（用于测试）
     */
    void mockPayment(String orderId);
}
