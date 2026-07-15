package com.algoviz.mapper;

import com.algoviz.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    
    /**
     * 根据订单ID获取订单
     */
    Order getOrderById(@Param("orderId") String orderId);
    
    /**
     * 根据用户ID获取订单列表
     */
    List<Order> getOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * 获取所有订单
     */
    List<Order> getAllOrders();
    
    /**
     * 根据状态获取订单
     */
    List<Order> getOrdersByStatus(@Param("status") String status);
    
    /**
     * 插入订单
     */
    int insertOrder(Order order);
    
    /**
     * 更新订单状态
     */
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);
    
    /**
     * 更新订单支付信息
     */
    int updateOrderPaymentInfo(@Param("orderId") String orderId, 
                               @Param("wechatTradeNo") String wechatTradeNo, 
                               @Param("wechatTransactionId") String wechatTransactionId);
    
    /**
     * 删除订单
     */
    int deleteOrder(@Param("orderId") String orderId);
    
    /**
     * 获取所有订单（含用户信息）
     */
    List<Order> getOrdersWithUserInfo();
    
    /**
     * 根据退款状态获取订单
     */
    List<Order> getOrdersByRefundStatus(@Param("refundStatus") String refundStatus);
    
    /**
     * 更新退款状态
     */
    int updateRefundStatus(@Param("orderId") String orderId, 
                           @Param("refundStatus") String refundStatus,
                           @Param("refundReason") String refundReason);
    
    /**
     * 完成退款
     */
    int completeRefund(@Param("orderId") String orderId);
}
