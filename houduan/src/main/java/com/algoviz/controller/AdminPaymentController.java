package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.PaymentRecord;
import com.algoviz.entity.PaymentTrend;
import com.algoviz.mapper.PaymentRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "支付管理(后台)", description = "支付记录/退款/统计")
public class AdminPaymentController {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @GetMapping("/payment/records")
    @Operation(summary = "查询支付记录", description = "分页查询支付记录列表")
    public ApiResponse<Map<String, Object>> getPaymentRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PaymentRecord> list = paymentRecordMapper.findByPage(offset, pageSize);
        int total = paymentRecordMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/records/{id}")
    @Operation(summary = "查询支付记录详情", description = "根据 ID 查询单条支付记录")
    public ApiResponse<PaymentRecord> getPaymentRecord(@PathVariable String id) {
        return ApiResponse.success(paymentRecordMapper.findById(id));
    }

    @GetMapping("/payment/refunds")
    @Operation(summary = "查询退款记录", description = "分页查询已退款的支付记录列表")
    public ApiResponse<Map<String, Object>> getRefundRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PaymentRecord> list = paymentRecordMapper.findRefundedRecords(offset, pageSize);
        int total = paymentRecordMapper.countRefunded();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/stats")
    @Operation(summary = "查询支付统计", description = "汇总收入、退款与订单总数")
    public ApiResponse<Map<String, Object>> getPaymentStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", paymentRecordMapper.sumTotalIncome());
        result.put("todayIncome", paymentRecordMapper.sumTodayIncome());
        result.put("weekIncome", paymentRecordMapper.sumWeekIncome());
        result.put("monthIncome", paymentRecordMapper.sumMonthIncome());
        result.put("totalRefund", paymentRecordMapper.sumTotalRefund());
        result.put("totalOrders", paymentRecordMapper.count());
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/trend")
    @Operation(summary = "查询收入趋势", description = "查询指定天数内的收入趋势数据")
    public ApiResponse<List<PaymentTrend>> getPaymentTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(paymentRecordMapper.getIncomeTrend(days));
    }
}
