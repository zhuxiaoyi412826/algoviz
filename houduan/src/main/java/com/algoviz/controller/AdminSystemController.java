package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.LoginLog;
import com.algoviz.entity.OperationLog;
import com.algoviz.entity.SystemConfig;
import com.algoviz.mapper.LoginLogMapper;
import com.algoviz.mapper.OperationLogMapper;
import com.algoviz.mapper.SystemConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * 基础配置的固定 keys（前后端统一约定）
     */
    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_SITE_LOGO = "site_logo";
    private static final String KEY_ICP_NUMBER = "icp_number";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_GITHUB_LINK = "github_link";
    private static final String KEY_SITE_SLOGAN = "site_slogan";

    /**
     * 系统启动时初始化 6 个基础配置项到 system_config（不存在则插入默认值）
     */
    @PostConstruct
    public void initBasicConfigs() {
        ensureConfig(KEY_SITE_NAME, "AlgoViz", "string", "站点名称", "前台站点/导航显示的名称", "basic");
        ensureConfig(KEY_SITE_LOGO, "", "string", "网站Logo", "站点图标（URL或上传后保存的路径）", "basic");
        ensureConfig(KEY_ICP_NUMBER, "豫ICP备12345678号", "string", "ICP备案号", "工信部备案号（用于页脚）", "basic");
        ensureConfig(KEY_COPYRIGHT, "© 2026 AlgoViz", "string", "版权信息", "页脚展示的版权声明", "basic");
        ensureConfig(KEY_GITHUB_LINK, "https://github.com/", "string", "GitHub链接", "页脚展示的仓库链接", "basic");
        ensureConfig(KEY_SITE_SLOGAN, "AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣", "string", "站点标语", "页脚展示的一句话介绍", "basic");
    }

    private void ensureConfig(String key, String defaultValue, String type, String label, String description, String group) {
        try {
            SystemConfig existed = systemConfigMapper.findByKey(key);
            if (existed == null) {
                SystemConfig c = new SystemConfig();
                c.setKey(key);
                c.setValue(defaultValue);
                c.setType(type);
                c.setLabel(label);
                c.setDescription(description);
                c.setConfigGroup(group);
                systemConfigMapper.insertOrUpdate(c);
            }
        } catch (Exception ignore) {
            // 数据库尚未初始化启动时，防止启动失败
        }
    }

    @GetMapping("/system/login-log")
    @Operation(summary = "查询登录日志", description = "分页查询登录日志，支持按用户名、状态、日期范围筛选")
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
    @Operation(summary = "查询登录日志统计", description = "统计今日、本周登录次数及成功/失败次数和失败率")
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
    @Operation(summary = "查询登录日志详情", description = "根据ID查询登录日志详情")
    public ApiResponse<LoginLog> getLoginLogDetail(@PathVariable String id) {
        return ApiResponse.success(loginLogMapper.findById(id));
    }

    @GetMapping("/system/operation-log")
    @Operation(summary = "查询操作日志", description = "分页查询操作日志，支持按用户名、模块、动作、日期范围筛选")
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
    @Operation(summary = "查询操作日志详情", description = "根据ID查询操作日志详情")
    public ApiResponse<OperationLog> getOperationLogDetail(@PathVariable String id) {
        return ApiResponse.success(operationLogMapper.findById(id));
    }

    /**
     * 操作日志 - 去重操作人下拉（供前端选择筛选）
     */
    @GetMapping("/system/operation-log/operators")
    @Operation(summary = "查询操作人列表", description = "查询去重后的操作人列表，供前端筛选下拉使用")
    public ApiResponse<List<String>> getOperationLogOperators() {
        return ApiResponse.success(operationLogMapper.findAllOperators());
    }

    @GetMapping("/system/config")
    @Operation(summary = "查询系统配置", description = "查询系统全部配置项列表")
    public ApiResponse<List<SystemConfig>> getSystemConfig() {
        return ApiResponse.success(systemConfigMapper.findAll());
    }

    @PutMapping("/system/config")
    @Operation(summary = "更新系统配置", description = "批量更新系统配置项，不存在的键自动新增")
    public ApiResponse<Void> updateSystemConfig(@RequestBody Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null) continue;
            SystemConfig existed = systemConfigMapper.findByKey(entry.getKey());
            String value = entry.getValue().toString();
            if (existed != null) {
                existed.setValue(value);
                systemConfigMapper.insertOrUpdate(existed);
            } else {
                SystemConfig c = new SystemConfig();
                c.setKey(entry.getKey());
                c.setValue(value);
                c.setType("string");
                c.setLabel(entry.getKey());
                c.setConfigGroup("basic");
                systemConfigMapper.insertOrUpdate(c);
            }
        }
        return ApiResponse.success(null);
    }

    private static final List<String> BASIC_KEYS_ORDERED;

    static {
        List<String> keys = new ArrayList<>();
        keys.add(KEY_SITE_NAME);
        keys.add(KEY_SITE_LOGO);
        keys.add(KEY_ICP_NUMBER);
        keys.add(KEY_COPYRIGHT);
        keys.add(KEY_GITHUB_LINK);
        keys.add(KEY_SITE_SLOGAN);
        BASIC_KEYS_ORDERED = Collections.unmodifiableList(keys);
    }

    private Map<String, String> buildBasicConfigMap() {
        Map<String, String> res = new HashMap<>();
        res.put("siteName", valueOrDefault(KEY_SITE_NAME, "AlgoViz"));
        res.put("siteLogo", valueOrDefault(KEY_SITE_LOGO, ""));
        res.put("icpNumber", valueOrDefault(KEY_ICP_NUMBER, ""));
        res.put("copyright", valueOrDefault(KEY_COPYRIGHT, "© 2026 AlgoViz"));
        res.put("githubLink", valueOrDefault(KEY_GITHUB_LINK, ""));
        res.put("siteSlogan", valueOrDefault(KEY_SITE_SLOGAN, "AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣"));
        return res;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    /**
     * 根据当前 system_config 的基础配置 value 计算 version（16 位 MD5 前缀）。
     * 任何一个 value 变化都会导致 version 不同；无需数据库改表即可实现版本号比对。
     */
    private String computeConfigVersion() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String k : BASIC_KEYS_ORDERED) {
                sb.append(k).append('=').append(valueOrDefault(k, "")).append('\n');
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(digest).substring(0, 16);
        } catch (Exception e) {
            // 兜底：用 values 的 hashCode 十六进制，任何 value 变化仍能区分
            int h = 0;
            for (String k : BASIC_KEYS_ORDERED) h = 31 * h + valueOrDefault(k, "").hashCode();
            return String.format("%016x", (long) h & 0xFFFFFFFFL);
        }
    }

    /**
     * 前台公开接口：获取站点基础配置（不鉴权，页脚用）
     * 返回: { version, siteName, siteLogo, icpNumber, copyright, githubLink, siteSlogan }
     */
    @GetMapping("/public/site-config")
    @Operation(summary = "获取站点基础配置", description = "前台公开接口，获取站点名称、Logo、备案号等基础配置及版本号")
    public ApiResponse<Map<String, String>> getPublicSiteConfig() {
        Map<String, String> res = buildBasicConfigMap();
        res.put("version", computeConfigVersion());
        return ApiResponse.success(res);
    }

    /**
     * 前台公开接口：只返回配置版本号（轻量轮询用，几十字节，避免频繁拉全量数据）。
     * 返回: { version: "xxxxxxxxxxxxxxxx" }
     */
    @GetMapping("/public/site-config/version")
    @Operation(summary = "获取站点配置版本号", description = "前台公开接口，仅返回配置版本号供前端轻量轮询")
    public ApiResponse<Map<String, String>> getPublicSiteConfigVersion() {
        Map<String, String> res = new HashMap<>(2);
        res.put("version", computeConfigVersion());
        return ApiResponse.success(res);
    }

    private String valueOrDefault(String key, String defaultValue) {
        try {
            SystemConfig c = systemConfigMapper.findByKey(key);
            return (c != null && c.getValue() != null) ? c.getValue() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
