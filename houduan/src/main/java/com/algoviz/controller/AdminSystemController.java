package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.LoginLog;
import com.algoviz.entity.OperationLog;
import com.algoviz.entity.SystemConfig;
import com.algoviz.mapper.LoginLogMapper;
import com.algoviz.mapper.OperationLogMapper;
import com.algoviz.mapper.SystemConfigMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "系统管理", description = "登录日志/操作日志/系统配置")
public class AdminSystemController {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @GetMapping("/system/login-log")
    public ApiResponse<Map<String, Object>> getLoginLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<LoginLog> list = loginLogMapper.findByPage(offset, pageSize);
        int total = loginLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/login-log/stats")
    public ApiResponse<Map<String, Object>> getLoginLogStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayCount", loginLogMapper.countToday());
        result.put("weekCount", loginLogMapper.countWeek());
        result.put("failCount", loginLogMapper.countFailed());
        return ApiResponse.success(result);
    }

    @GetMapping("/system/operation-log")
    public ApiResponse<Map<String, Object>> getOperationLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OperationLog> list = operationLogMapper.findByPage(offset, pageSize);
        int total = operationLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/operation-log/{id}")
    public ApiResponse<OperationLog> getOperationLogDetail(@PathVariable String id) {
        return ApiResponse.success(operationLogMapper.findById(id));
    }

    @GetMapping("/system/config")
    public ApiResponse<List<SystemConfig>> getSystemConfig() {
        return ApiResponse.success(systemConfigMapper.findAll());
    }

    @PutMapping("/system/config")
    public ApiResponse<Void> updateSystemConfig(@RequestBody Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            SystemConfig config = systemConfigMapper.findByKey(entry.getKey());
            if (config != null) {
                config.setValue(entry.getValue().toString());
                systemConfigMapper.insertOrUpdate(config);
            }
        }
        return ApiResponse.success(null);
    }
}