package com.algoviz.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /**
     * 合并查询1：user 表一次性统计（替代原来 8 次全表扫描）
     * 返回：totalUsers, totalAIDialogues, todayAIDialogues, yesterdayAIDialogues,
     *       dsVisits, algoVisits, ojVisits, aiVisits
     */
    @Select("SELECT " +
            "COUNT(*) AS totalUsers, " +
            "IFNULL(SUM(ai_dialogues), 0) AS totalAIDialogues, " +
            "IFNULL(SUM(CASE WHEN last_visit_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) THEN ai_dialogues ELSE 0 END), 0) AS todayAIDialogues, " +
            "IFNULL(SUM(CASE WHEN last_visit_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND last_visit_time < CURDATE() THEN ai_dialogues ELSE 0 END), 0) AS yesterdayAIDialogues, " +
            "IFNULL(SUM(ds_visits), 0) AS dsVisits, " +
            "IFNULL(SUM(algo_visits), 0) AS algoVisits, " +
            "IFNULL(SUM(oj_visits), 0) AS ojVisits " +
            "FROM user")
    Map<String, Object> getUserStats();

    /**
     * 合并查询2：submission 表一次性统计
     * 返回：totalSubmissions, todaySubmissions, yesterdaySubmissions, todayOJStatusDist
     */
    @Select("SELECT " +
            "COUNT(*) AS totalSubmissions, " +
            "IFNULL(SUM(CASE WHEN submit_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) THEN 1 ELSE 0 END), 0) AS todaySubmissions, " +
            "IFNULL(SUM(CASE WHEN submit_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND submit_time < CURDATE() THEN 1 ELSE 0 END), 0) AS yesterdaySubmissions " +
            "FROM submission")
    Map<String, Object> getSubmissionStats();

    // === 今日活跃用户（走 login_log 索引） ===
    @Select("SELECT COUNT(DISTINCT user_id) FROM login_log WHERE login_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) AND status = 'success'")
    int countTodayActiveUsers();

    // === 昨日活跃用户 ===
    @Select("SELECT COUNT(DISTINCT user_id) FROM login_log WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND login_time < CURDATE() AND status = 'success'")
    int countYesterdayActiveUsers();

    // === 各模块题目数（小表，快） ===
    @Select("SELECT COUNT(*) FROM oj_problem")
    int countOJProblems();

    @Select("SELECT COUNT(*) FROM interview_problem WHERE status = 'ACTIVE' AND (is_deleted = 0 OR is_deleted IS NULL)")
    int countInterviewProblems();

    @Select("SELECT COUNT(*) FROM algorithm")
    int countAlgorithms();

    @Select("SELECT COUNT(*) FROM data_structure")
    int countDataStructures();

    /**
     * 趋势数据（近7天）- 优化为 GROUP BY 而非关联子查询
     * 用 LEFT JOIN + GROUP BY 一次查出 7 天数据
     */
    @Select("SELECT dates.d AS date, " +
            "IFNULL(l.dau, 0) AS dau, " +
            "IFNULL(s.submissions, 0) AS submissions " +
            "FROM (" +
            "  SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS d " +
            "  FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days" +
            ") dates " +
            "LEFT JOIN (" +
            "  SELECT DATE(login_time) AS dt, COUNT(DISTINCT user_id) AS dau " +
            "  FROM login_log WHERE status = 'success' AND login_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "  GROUP BY DATE(login_time)" +
            ") l ON l.dt = dates.d " +
            "LEFT JOIN (" +
            "  SELECT DATE(submit_time) AS dt, COUNT(*) AS submissions " +
            "  FROM submission WHERE submit_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "  GROUP BY DATE(submit_time)" +
            ") s ON s.dt = dates.d " +
            "ORDER BY dates.d")
    List<Map<String, Object>> getWeekTrend();

    // === OJ 判题分布 ===
    @Select("SELECT status as name, COUNT(*) as value FROM submission GROUP BY status")
    List<Map<String, Object>> getOJStatusDistribution();

    // === 最多提交的 OJ 题目 Top 5 ===
    @Select("SELECT p.title AS title, p.submission_count AS submissions, p.difficulty AS difficulty " +
            "FROM oj_problem p ORDER BY p.submission_count DESC LIMIT 5")
    List<Map<String, Object>> getTopOJProblems();

    // === 最多浏览的面试题 Top 5 ===
    @Select("SELECT p.title AS title, p.view_count AS views, p.difficulty AS difficulty, p.category AS category " +
            "FROM interview_problem p WHERE p.status = 'ACTIVE' AND (p.is_deleted = 0 OR p.is_deleted IS NULL) " +
            "ORDER BY p.view_count DESC LIMIT 5")
    List<Map<String, Object>> getTopInterviewProblems();
}
