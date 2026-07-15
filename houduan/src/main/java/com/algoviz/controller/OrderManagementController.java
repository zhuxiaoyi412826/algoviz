package com.algoviz.controller;

import com.algoviz.entity.Order;
import com.algoviz.mapper.OrderMapper;
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
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "订单管理相关接口")
public class OrderManagementController {

    private static final Logger logger = LoggerFactory.getLogger(OrderManagementController.class);

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping
    @Operation(summary = "获取订单列表", description = "获取所有订单（含用户信息）")
    public Map<String, Object> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refundStatus) {
        
        logger.info("获取订单列表 - 状态: {}, 退款状态: {}", status, refundStatus);
        
        Map<String, Object> result = new HashMap<>();
        List<Order> orders;

        if (refundStatus != null && !refundStatus.isEmpty()) {
            orders = orderMapper.getOrdersByRefundStatus(refundStatus);
        } else if (status != null && !status.isEmpty()) {
            orders = orderMapper.getOrdersByStatus(status);
        } else {
            orders = orderMapper.getOrdersWithUserInfo();
        }

        result.put("success", true);
        result.put("orders", orders);
        result.put("count", orders.size());
        
        return result;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详情")
    public Map<String, Object> getOrderById(@PathVariable String orderId) {
        logger.info("获取订单详情：{}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        Order order = orderMapper.getOrderById(orderId);
        
        if (order != null) {
            result.put("success", true);
            result.put("order", order);
        } else {
            result.put("success", false);
            result.put("message", "订单不存在");
        }
        
        return result;
    }

    @PutMapping("/{orderId}/refund/apply")
    @Operation(summary = "申请退款", description = "用户申请退款")
    public Map<String, Object> applyRefund(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        logger.info("用户申请退款：{}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        String reason = body.get("reason");
        
        try {
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            
            if (!"SUCCESS".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "只有已支付的订单才能申请退款");
                return result;
            }
            
            if ("APPLIED".equals(order.getRefundStatus())) {
                result.put("success", false);
                result.put("message", "该订单已申请退款，请勿重复申请");
                return result;
            }
            
            orderMapper.updateRefundStatus(orderId, "APPLIED", reason);
            result.put("success", true);
            result.put("message", "退款申请已提交，等待审核");
            
        } catch (Exception e) {
            logger.error("申请退款失败", e);
            result.put("success", false);
            result.put("message", "申请退款失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{orderId}/refund/approve")
    @Operation(summary = "审批退款", description = "管理员审批退款申请")
    public Map<String, Object> approveRefund(@PathVariable String orderId) {
        logger.info("审批退款：{}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            
            if (!"APPLIED".equals(order.getRefundStatus())) {
                result.put("success", false);
                result.put("message", "该订单没有待审核的退款申请");
                return result;
            }
            
            // 调用微信退款API（模拟）
            // 实际项目中需要调用微信退款接口
            boolean refundSuccess = simulateWechatRefund(order);
            
            if (refundSuccess) {
                orderMapper.completeRefund(orderId);
                result.put("success", true);
                result.put("message", "退款成功");
            } else {
                orderMapper.updateRefundStatus(orderId, "FAILED", "退款失败");
                result.put("success", false);
                result.put("message", "退款失败，请重试");
            }
            
        } catch (Exception e) {
            logger.error("审批退款失败", e);
            result.put("success", false);
            result.put("message", "审批退款失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{orderId}/refund/reject")
    @Operation(summary = "拒绝退款", description = "管理员拒绝退款申请")
    public Map<String, Object> rejectRefund(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        logger.info("拒绝退款：{}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        String reason = body.get("reason");
        
        try {
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            
            if (!"APPLIED".equals(order.getRefundStatus())) {
                result.put("success", false);
                result.put("message", "该订单没有待审核的退款申请");
                return result;
            }
            
            orderMapper.updateRefundStatus(orderId, "REJECTED", reason);
            result.put("success", true);
            result.put("message", "已拒绝退款申请");
            
        } catch (Exception e) {
            logger.error("拒绝退款失败", e);
            result.put("success", false);
            result.put("message", "拒绝退款失败：" + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/stats")
    @Operation(summary = "订单统计", description = "获取订单统计信息")
    public Map<String, Object> getOrderStats() {
        logger.info("获取订单统计");
        
        Map<String, Object> result = new HashMap<>();
        
        List<Order> allOrders = orderMapper.getAllOrders();
        long pendingCount = allOrders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long successCount = allOrders.stream().filter(o -> "SUCCESS".equals(o.getStatus())).count();
        long appliedRefundCount = allOrders.stream().filter(o -> "APPLIED".equals(o.getRefundStatus())).count();
        long refundedCount = allOrders.stream().filter(o -> "REFUNDED".equals(o.getRefundStatus())).count();
        
        int totalAmount = allOrders.stream().mapToInt(Order::getAmount).sum();
        int successAmount = allOrders.stream().filter(o -> "SUCCESS".equals(o.getStatus())).mapToInt(Order::getAmount).sum();
        
        result.put("success", true);
        result.put("totalCount", allOrders.size());
        result.put("pendingCount", pendingCount);
        result.put("successCount", successCount);
        result.put("appliedRefundCount", appliedRefundCount);
        result.put("refundedCount", refundedCount);
        result.put("totalAmount", totalAmount);
        result.put("successAmount", successAmount);
        
        return result;
    }

    /**
     * 模拟微信退款（实际项目中需要调用真实的微信退款API）
     */
    private boolean simulateWechatRefund(Order order) {
        // 模拟退款操作
        logger.info("模拟微信退款：订单号={}, 金额={}", order.getOrderId(), order.getAmount());
        return true; // 模拟成功
    }
}