package com.algoviz.mapper;

import com.algoviz.entity.PaymentRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PaymentRecordMapper {
    @Insert("INSERT INTO payment_record (id, order_id, product_id, product_name, amount, payment_method, transaction_id, status, refund_status, refund_reason, create_time, pay_time, refund_time) " +
            "VALUES (#{id}, #{orderId}, #{productId}, #{productName}, #{amount}, #{paymentMethod}, #{transactionId}, #{status}, #{refundStatus}, #{refundReason}, #{createTime}, #{payTime}, #{refundTime})")
    int insert(PaymentRecord record);

    @Select("SELECT * FROM payment_record ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<PaymentRecord> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM payment_record")
    int count();

    @Select("SELECT * FROM payment_record WHERE id = #{id}")
    PaymentRecord findById(String id);

    @Select("SELECT * FROM payment_record WHERE order_id = #{orderId}")
    PaymentRecord findByOrderId(String orderId);

    @Select("SELECT * FROM payment_record WHERE refund_status = 'refunded' ORDER BY refund_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<PaymentRecord> findRefundedRecords(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM payment_record WHERE refund_status = 'refunded'")
    int countRefunded();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment_record WHERE status = 'paid'")
    Integer sumTotalIncome();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment_record WHERE status = 'paid' AND date(create_time) = date('now')")
    Integer sumTodayIncome();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment_record WHERE status = 'paid' AND date(create_time) >= date('now', '-7 day')")
    Integer sumWeekIncome();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment_record WHERE status = 'paid' AND date(create_time) >= date('now', '-30 day')")
    Integer sumMonthIncome();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment_record WHERE refund_status = 'refunded'")
    Integer sumTotalRefund();

    @Select("SELECT date(create_time) as date, SUM(amount) as amount FROM payment_record WHERE status = 'paid' GROUP BY date(create_time) ORDER BY date DESC LIMIT #{limit}")
    List<com.algoviz.entity.PaymentTrend> getIncomeTrend(int limit);

    @Update("UPDATE payment_record SET status=#{status}, pay_time=#{payTime}, transaction_id=#{transactionId} WHERE id=#{id}")
    int updatePaymentStatus(@Param("id") String id, @Param("status") String status, @Param("payTime") String payTime, @Param("transactionId") String transactionId);

    @Update("UPDATE payment_record SET refund_status=#{refundStatus}, refund_reason=#{refundReason}, refund_time=#{refundTime} WHERE id=#{id}")
    int updateRefundStatus(@Param("id") String id, @Param("refundStatus") String refundStatus, @Param("refundReason") String refundReason, @Param("refundTime") String refundTime);
}
