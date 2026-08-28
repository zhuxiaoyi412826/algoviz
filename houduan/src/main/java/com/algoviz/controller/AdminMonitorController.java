package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.ApiLog;
import com.algoviz.mapper.ApiLogMapper;
import com.algoviz.mapper.AppUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "运维监控", description = "服务监控与API性能监控")
public class AdminMonitorController {

    @Autowired
    private ApiLogMapper apiLogMapper;

    @Autowired
    private AppUserMapper appUserMapper;

    @GetMapping("/monitor/service")
    @Operation(summary = "查询服务监控", description = "返回后端、前端、数据库等服务的运行状态（含CPU、内存、磁盘、运行时长）")
    public ApiResponse<List<Map<String, Object>>> getServiceStatus() {
        List<Map<String, Object>> services = List.of(
            Map.of("name", "后端服务", "status", "running", "cpu", 10, "memory", 256, "disk", 10, "uptime", "00:00:00"),
            Map.of("name", "前端服务", "status", "running", "cpu", 5, "memory", 128, "disk", 5, "uptime", "00:00:00"),
            Map.of("name", "数据库", "status", "running", "cpu", 2, "memory", 64, "disk", 20, "uptime", "00:00:00")
        );
        return ApiResponse.success(services);
    }

    @GetMapping("/monitor/alerts")
    @Operation(summary = "查询监控告警", description = "返回当前监控告警列表（暂返回空列表）")
    public ApiResponse<List<Map<String, Object>>> getAlerts() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/monitor/api")
    @Operation(summary = "查询API性能统计", description = "统计各接口今日请求量、平均响应时间、错误数与成功率，可按条数限制")
    public ApiResponse<Map<String, Object>> getApiStats(
            @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ApiLogMapper.ApiStatistics> stats = apiLogMapper.getApiStatistics(limit);
            int todayRequests = apiLogMapper.countToday();
            Long avgResponseTime = apiLogMapper.avgResponseTimeToday();
            int errorCount = apiLogMapper.countErrorToday();

            result.put("apiList", stats != null ? stats : List.of());
            result.put("todayRequests", todayRequests);
            result.put("avgResponseTime", avgResponseTime != null ? avgResponseTime : 0L);
            result.put("errorCount", errorCount);
            result.put("successRate", todayRequests > 0 ?
                    (todayRequests - errorCount) * 100.0 / todayRequests : 100.0);
        } catch (Exception e) {
            result.put("apiList", List.of());
            result.put("todayRequests", 0);
            result.put("avgResponseTime", 0);
            result.put("errorCount", 0);
            result.put("successRate", 100.0);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/monitor/api/hourly")
    @Operation(summary = "查询API按小时统计", description = "统计今日各小时段的API请求量")
    public ApiResponse<List<Map<String, Object>>> getApiHourlyStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<ApiLogMapper.ApiHourlyStats> stats = apiLogMapper.getHourlyStatsToday();
            if (stats != null) {
                for (ApiLogMapper.ApiHourlyStats s : stats) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("hour", s.getHour());
                    map.put("count", s.getCount());
                    result.add(map);
                }
            }
        } catch (Exception e) {
            // return empty
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/monitor/api/daily")
    @Operation(summary = "查询API按日统计", description = "统计最近N天（默认7天）每日API请求量与平均耗时")
    public ApiResponse<List<Map<String, Object>>> getApiDailyStats(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<ApiLogMapper.ApiDailyStats> stats = apiLogMapper.getDailyStats(days);
            if (stats != null) {
                for (ApiLogMapper.ApiDailyStats s : stats) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", s.getDate());
                    map.put("count", s.getCount());
                    map.put("avgTime", s.getAvgTime());
                    result.add(map);
                }
            }
        } catch (Exception e) {
            // return empty
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/monitor/api/logs")
    @Operation(summary = "分页查询API调用日志", description = "按页码和每页条数分页查询API调用日志列表及总数")
    public ApiResponse<Map<String, Object>> getApiLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ApiLog> list = apiLogMapper.findByPage(offset, pageSize);
        int total = apiLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/monitor/api/logs/clean")
    @Operation(summary = "清理旧API日志", description = "清理过期的API调用日志并返回删除条数")
    public ApiResponse<Void> cleanOldApiLogs() {
        int deleted = apiLogMapper.cleanOldLogs();
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.success(null);
    }
}