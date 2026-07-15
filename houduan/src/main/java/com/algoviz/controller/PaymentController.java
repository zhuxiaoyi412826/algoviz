package com.algoviz.controller;

import com.algoviz.dto.CreateOrderRequest;
import com.algoviz.dto.CreateOrderResponse;
import com.algoviz.dto.QueryOrderResponse;
import com.algoviz.entity.Product;
import com.algoviz.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "支付管理", description = "微信支付相关接口")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 获取商品列表
     */
    @GetMapping("/products")
    @Operation(summary = "获取商品列表", description = "获取所有可购买的商品列表")
    public Map<String, Object> getProducts() {
        logger.info("获取商品列表");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("products", paymentService.getProductList());
        return result;
    }
    
    /**
     * 获取商品详情
     */
    @GetMapping("/products/{productId}")
    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详情")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        logger.info("获取商品详情：{}", productId);
        Map<String, Object> result = new HashMap<>();
        Product product = paymentService.getProduct(productId);
        
        if (product != null) {
            result.put("success", true);
            result.put("product", product);
        } else {
            result.put("success", false);
            result.put("message", "商品不存在");
        }
        
        return result;
    }
    
    /**
     * 创建支付订单
     */
    @PostMapping("/order")
    @Operation(summary = "创建订单", description = "创建微信支付订单")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        logger.info("创建订单，商品ID：{}", request.getProductId());
        
        // 获取用户ID（实际项目中从登录状态获取）
        Long userId = 1L;
        
        return paymentService.createOrder(request.getProductId(), userId);
    }
    
    /**
     * 查询订单状态
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单", description = "查询支付订单状态")
    public QueryOrderResponse queryOrder(@PathVariable String orderId) {
        logger.info("查询订单：{}", orderId);
        return paymentService.queryOrder(orderId);
    }
    
    /**
     * 模拟支付（用于测试）
     */
    @PostMapping("/order/{orderId}/mock-pay")
    @Operation(summary = "模拟支付", description = "模拟微信支付成功（测试用）")
    public Map<String, Object> mockPayment(@PathVariable String orderId) {
        logger.info("模拟支付：{}", orderId);
        paymentService.mockPayment(orderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "模拟支付成功");
        return result;
    }
    
    /**
     * 支付回调接口
     */
    @PostMapping("/callback")
    @Operation(summary = "支付回调", description = "微信支付回调通知接口")
    public Map<String, Object> paymentCallback(@RequestBody(required = false) String jsonData,
                                               @RequestHeader Map<String, String> headers) {
        logger.info("========== 收到支付回调请求 ==========");
        logger.info("请求头信息：");
        headers.forEach((key, value) -> {
            logger.info("  {}: {}", key, value);
        });
        
        if (jsonData == null || jsonData.isEmpty()) {
            logger.warn("回调请求体为空");
        } else {
            logger.info("回调请求体长度：{} 字符", jsonData.length());
            logger.info("回调请求体内容：{}", jsonData);
        }
        
        try {
            paymentService.handlePaymentCallback(jsonData != null ? jsonData : "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            logger.info("支付回调处理成功，返回SUCCESS");
            return result;
        } catch (Exception e) {
            logger.error("处理支付回调失败：", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", "FAIL");
            result.put("message", "失败");
            return result;
        }
    }
}
