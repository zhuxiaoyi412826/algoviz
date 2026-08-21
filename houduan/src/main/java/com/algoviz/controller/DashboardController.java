package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.mapper.DashboardMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "后台首页数据聚合接口")
public class DashboardController {

    @Autowired
    private DashboardMapper dashboardMapper;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            // === 概览卡片 ===
            int totalUsers = dashboardMapper.countUsers();
            int todayActive = dashboardMapper.countTodayActiveUsers();
            int yesterdayActive = dashboardMapper.countYesterdayActiveUsers();
            int todaySubmissions = dashboardMapper.countTodaySubmissions();
            int yesterdaySubmissions = dashboardMapper.countYesterdaySubmissions();
            int todayAIDialogues = dashboardMapper.countTodayAIDialogues();
            int yesterdayAIDialogues = dashboardMapper.countYesterdayAIDialogues();
            int totalAIDialogues = dashboardMapper.countAIDialogues();
            int ojProblems = dashboardMapper.countOJProblems();
            int interviewProblems = dashboardMapper.countInterviewProblems();
            int algorithms = dashboardMapper.countAlgorithms();
            int dataStructures = dashboardMapper.countDataStructures();
            int totalSubmissions = dashboardMapper.countTotalSubmissions();

            // === 趋势 ===
            List<Map<String, Object>> weekTrend = dashboardMapper.getWeekTrend();

            // === OJ 判题分布 ===
            List<Map<String, Object>> ojDistribution = dashboardMapper.getOJStatusDistribution();

            // === 模块访问量 ===
            List<Map<String, Object>> moduleVisits = dashboardMapper.getModuleVisits();

            // === Top 5 ===
            List<Map<String, Object>> topOJProblems = dashboardMapper.getTopOJProblems();
            List<Map<String, Object>> topInterviewProblems = dashboardMapper.getTopInterviewProblems();

            // 计算环比变化
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
            result.put("moduleVisits", moduleVisits);
            result.put("topOJProblems", topOJProblems);
            result.put("topInterviewProblems", topInterviewProblems);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }
}
