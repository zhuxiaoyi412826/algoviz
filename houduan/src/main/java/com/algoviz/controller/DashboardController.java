package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.mapper.DashboardMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "后台首页数据聚合接口")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardMapper dashboardMapper;

    /** 仪表盘缓存（5 分钟 TTL） */
    private volatile Map<String, Object> cachedStats;
    private volatile long cacheExpireAt;
    private static final long CACHE_TTL_MS = 300_000L; // 5 分钟

    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> getStats() {
        long totalStart = System.currentTimeMillis();
        long t0 = totalStart;
        boolean useCache = false;

        // 检查缓存
        long now = System.currentTimeMillis();
        if (cachedStats != null && now < cacheExpireAt) {
            long cost = System.currentTimeMillis() - totalStart;
            log.info("[DASHBOARD-stats] CACHE HIT total={}ms", cost);
            return ApiResponse.success(cachedStats);
        }
        log.info("[DASHBOARD-stats] CACHE MISS -> start computing");

        Map<String, Object> result = new HashMap<>();
        try {
            // === 合并查询1：user 表统计（1 次扫描替代原来 8 次） ===
            Map<String, Object> userStats = dashboardMapper.getUserStats();
            long t1 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step1 getUserStats={}ms", t1 - t0);
            t0 = t1;
            long totalUsers = toLong(userStats.get("totalUsers"));
            long totalAIDialogues = toLong(userStats.get("totalAIDialogues"));
            long todayAIDialogues = toLong(userStats.get("todayAIDialogues"));
            long yesterdayAIDialogues = toLong(userStats.get("yesterdayAIDialogues"));
            long dsVisits = toLong(userStats.get("dsVisits"));
            long algoVisits = toLong(userStats.get("algoVisits"));
            long ojVisits = toLong(userStats.get("ojVisits"));

            // === 合并查询2：submission 表统计（1 次扫描替代原来 3 次） ===
            Map<String, Object> subStats = dashboardMapper.getSubmissionStats();
            long t2 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step2 getSubmissionStats={}ms", t2 - t0);
            t0 = t2;
            long totalSubmissions = toLong(subStats.get("totalSubmissions"));
            long todaySubmissions = toLong(subStats.get("todaySubmissions"));
            long yesterdaySubmissions = toLong(subStats.get("yesterdaySubmissions"));

            // === 活跃用户 ===
            int todayActive = dashboardMapper.countTodayActiveUsers();
            long t3 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step3 countTodayActive={}ms", t3 - t0);
            t0 = t3;
            int yesterdayActive = dashboardMapper.countYesterdayActiveUsers();
            long t4 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step4 countYesterdayActive={}ms", t4 - t0);
            t0 = t4;

            // === 模块题目数（小表，快） ===
            int ojProblems = dashboardMapper.countOJProblems();
            int interviewProblems = dashboardMapper.countInterviewProblems();
            int algorithms = dashboardMapper.countAlgorithms();
            int dataStructures = dashboardMapper.countDataStructures();
            long t5 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step5 problem counts={}ms (oj intv algo ds)", t5 - t0);
            t0 = t5;

            // === 趋势 + 分布 + Top5 ===
            List<Map<String, Object>> weekTrend = dashboardMapper.getWeekTrend();
            long t6 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step6 getWeekTrend={}ms", t6 - t0);
            t0 = t6;
            List<Map<String, Object>> ojDistribution = dashboardMapper.getOJStatusDistribution();
            long t7 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step7 getOJDistribution={}ms", t7 - t0);
            t0 = t7;
            List<Map<String, Object>> topOJProblems = dashboardMapper.getTopOJProblems();
            long t8 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step8 getTopOJProblems={}ms", t8 - t0);
            t0 = t8;
            List<Map<String, Object>> topInterviewProblems = dashboardMapper.getTopInterviewProblems();
            long t9 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step9 getTopInterviewProblems={}ms", t9 - t0);
            t0 = t9;

            // 环比变化
            double dauChange = yesterdayActive > 0 ? (todayActive - yesterdayActive) * 100.0 / yesterdayActive : 0;
            double submissionChange = yesterdaySubmissions > 0 ? (todaySubmissions - yesterdaySubmissions) * 100.0 / yesterdaySubmissions : 0;
            double aiChange = yesterdayAIDialogues > 0 ? (todayAIDialogues - yesterdayAIDialogues) * 100.0 / yesterdayAIDialogues : 0;

            Map<String, Object> overview = new HashMap<>();
            overview.put("totalUsers", totalUsers);
            overview.put("todayActive", todayActive);
            overview.put("todayActiveChange", Math.round(dauChange * 10.0) / 10.0);
            overview.put("todaySubmissions", todaySubmissions);
            overview.put("todaySubmissionsChange", Math.round(submissionChange * 10.0) / 10.0);
            overview.put("todayAIDialogues", todayAIDialogues);
            overview.put("todayAIDialoguesChange", Math.round(aiChange * 10.0) / 10.0);
            overview.put("totalSubmissions", totalSubmissions);
            overview.put("totalAIDialogues", totalAIDialogues);
            overview.put("ojProblems", ojProblems);
            overview.put("interviewProblems", interviewProblems);
            overview.put("algorithms", algorithms);
            overview.put("dataStructures", dataStructures);

            result.put("overview", overview);
            result.put("weekTrend", weekTrend);
            result.put("ojDistribution", ojDistribution);

            // 模块访问量（从合并查询结果中取）
            List<Map<String, Object>> moduleVisits = new ArrayList<>();
            moduleVisits.add(Map.of("name", "数据结构", "value", dsVisits));
            moduleVisits.add(Map.of("name", "算法", "value", algoVisits));
            moduleVisits.add(Map.of("name", "OJ", "value", ojVisits));
            moduleVisits.add(Map.of("name", "AI助手", "value", totalAIDialogues));
            result.put("moduleVisits", moduleVisits);

            result.put("topOJProblems", topOJProblems);
            result.put("topInterviewProblems", topInterviewProblems);

            // 写入缓存
            cachedStats = result;
            cacheExpireAt = now + CACHE_TTL_MS;

            long t10 = System.currentTimeMillis();
            log.info("[DASHBOARD-stats] step10 组装JSON+写缓存={}ms", t10 - t0);
            log.info("[DASHBOARD-stats] TOTAL COST={}ms (cache miss, computed)", t10 - totalStart);

        } catch (Exception e) {
            long failedAt = System.currentTimeMillis();
            log.error("[DASHBOARD-stats] FAILED total={}ms err={}", failedAt - totalStart, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    private long toLong(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0; }
    }
}
