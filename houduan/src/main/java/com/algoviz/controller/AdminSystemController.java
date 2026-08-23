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
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (pageSize > 100) pageSize = 100;
        int offset = (page - 1) * pageSize;
        List<LoginLog> list;
        int total;

        boolean hasUsername = username != null && !username.isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();
        boolean hasDateRange = startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty();

        if (!hasUsername && !hasStatus && !hasDateRange) {
            list = loginLogMapper.findByPage(offset, pageSize);
            total = loginLogMapper.count();
        } else if (hasUsername && hasStatus && hasDateRange) {
            list = loginLogMapper.findByAllFilters(offset, pageSize, username, status, startDate, endDate);
            total = loginLogMapper.countByAllFilters(username, status, startDate, endDate);
        } else if (hasUsername && hasStatus) {
            list = loginLogMapper.findByUsernameAndStatus(offset, pageSize, username, status);
            total = loginLogMapper.countByUsernameAndStatus(username, status);
        } else if (hasUsername && hasDateRange) {
            list = loginLogMapper.findByUsernameAndDateRange(offset, pageSize, username, startDate, endDate);
            total = loginLogMapper.countByUsernameAndDateRange(username, startDate, endDate);
        } else if (hasStatus && hasDateRange) {
            list = loginLogMapper.findByStatusAndDateRange(offset, pageSize, status, startDate, endDate);
            total = loginLogMapper.countByStatusAndDateRange(status, startDate, endDate);
        } else if (hasUsername) {
            list = loginLogMapper.findByUsername(offset, pageSize, username);
            total = loginLogMapper.countByUsername(username);
        } else if (hasStatus) {
            list = loginLogMapper.findByStatus(offset, pageSize, status);
            total = loginLogMapper.countByStatus(status);
        } else {
            list = loginLogMapper.findByDateRange(offset, pageSize, startDate, endDate);
            total = loginLogMapper.countByDateRange(startDate, endDate);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/login-log/stats")
    public ApiResponse<Map<String, Object>> getLoginLogStats() {
        Map<String, Object> result = new HashMap<>();
        int failCount = loginLogMapper.countFailed();
        int successCount = loginLogMapper.countSuccess();
        int total = failCount + successCount;
        result.put("todayCount", loginLogMapper.countToday());
        result.put("weekCount", loginLogMapper.countWeek());
        result.put("failCount", failCount);
        result.put("successCount", successCount);
        result.put("total", total);
        result.put("failRate", total > 0 ? Math.round(failCount * 10000.0 / total) / 100.0 : 0.0);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/login-log/{id}")
    public ApiResponse<LoginLog> getLoginLogDetail(@PathVariable String id) {
        return ApiResponse.success(loginLogMapper.findById(id));
    }

    @GetMapping("/system/operation-log")
    public ApiResponse<Map<String, Object>> getOperationLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        int offset = (page - 1) * pageSize;
        List<OperationLog> list;
        int total;

        boolean hasFilter = (username != null && !username.isEmpty())
                || (module != null && !module.isEmpty())
                || (action != null && !action.isEmpty())
                || (startDate != null && !startDate.isEmpty())
                || (endDate != null && !endDate.isEmpty());

        if (hasFilter) {
            list = operationLogMapper.findByAllFilters(offset, pageSize, username, module, action, startDate, endDate);
            total = operationLogMapper.countByAllFilters(username, module, action, startDate, endDate);
        } else {
            list = operationLogMapper.findByPage(offset, pageSize);
            total = operationLogMapper.count();
        }

        // 兼容前端：createdAt -> createTime
        List<Map<String, Object>> resultList = list.stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("userId", log.getUserId());
            item.put("username", log.getUsername());
            item.put("module", log.getModule());
            item.put("action", log.getAction());
            item.put("detail", log.getDetail());
            item.put("ip", log.getIp());
            item.put("createdAt", log.getCreatedAt());
            item.put("createTime", log.getCreatedAt());  // 前端用 createTime
            return item;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/operation-log/{id}")
    public ApiResponse<OperationLog> getOperationLogDetail(@PathVariable String id) {
        return ApiResponse.success(operationLogMapper.findById(id));
    }

    /**
     * 操作日志 - 去重操作人下拉（供前端选择筛选）
     */
    @GetMapping("/system/operation-log/operators")
    public ApiResponse<List<String>> getOperationLogOperators() {
        return ApiResponse.success(operationLogMapper.findAllOperators());
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