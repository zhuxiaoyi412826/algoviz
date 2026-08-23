package com.algoviz.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    // === 概览卡片 ===

    @Select("SELECT COUNT(*) FROM user")
    int countUsers();

    @Select("SELECT COUNT(DISTINCT user_id) FROM login_log WHERE login_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) AND status = 'success'")
    int countTodayActiveUsers();

    @Select("SELECT COUNT(DISTINCT user_id) FROM login_log WHERE login_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND status = 'success'")
    int countWeekActiveUsers();

    @Select("SELECT COUNT(*) FROM submission WHERE submit_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    int countTodaySubmissions();

    @Select("SELECT COUNT(*) FROM submission")
    int countTotalSubmissions();

    @Select("SELECT COUNT(*) FROM oj_problem")
    int countOJProblems();

    @Select("SELECT COUNT(*) FROM interview_problem WHERE status = 'ACTIVE' AND (is_deleted = 0 OR is_deleted IS NULL)")
    int countInterviewProblems();

    @Select("SELECT COUNT(*) FROM algorithm")
    int countAlgorithms();

    @Select("SELECT COUNT(*) FROM data_structure")
    int countDataStructures();

    @Select("SELECT IFNULL(SUM(ai_dialogues), 0) FROM user")
    int countAIDialogues();

    @Select("SELECT IFNULL(SUM(ai_dialogues), 0) FROM user WHERE last_visit_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    int countTodayAIDialogues();

    // === 趋势数据（近7天） ===

    @Select("SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS date, " +
            "(SELECT COUNT(DISTINCT user_id) FROM login_log WHERE status='success' AND login_time >= DATE_SUB(CURDATE(), INTERVAL n DAY) AND login_time < DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 1 DAY)) AS dau, " +
            "(SELECT COUNT(*) FROM submission WHERE submit_time >= DATE_SUB(CURDATE(), INTERVAL n DAY) AND submit_time < DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 1 DAY)) AS submissions " +
            "FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days " +
            "ORDER BY date")
    List<Map<String, Object>> getWeekTrend();

    // === OJ 判题分布 ===

    @Select("SELECT status as name, COUNT(*) as value FROM submission GROUP BY status")
    List<Map<String, Object>> getOJStatusDistribution();

    // === 模块访问量 ===

    @Select("SELECT '数据结构' AS name, IFNULL(SUM(ds_visits), 0) AS value FROM user " +
            "UNION ALL " +
            "SELECT '算法' AS name, IFNULL(SUM(algo_visits), 0) AS value FROM user " +
            "UNION ALL " +
            "SELECT 'OJ' AS name, IFNULL(SUM(oj_visits), 0) AS value FROM user " +
            "UNION ALL " +
            "SELECT 'AI助手' AS name, IFNULL(SUM(ai_dialogues), 0) AS value FROM user")
    List<Map<String, Object>> getModuleVisits();

    // === 最多提交的 OJ 题目 Top 5 ===

    @Select("SELECT p.title AS title, p.submission_count AS submissions, p.difficulty AS difficulty " +
            "FROM oj_problem p ORDER BY p.submission_count DESC LIMIT 5")
    List<Map<String, Object>> getTopOJProblems();

    // === 最多浏览的面试题 Top 5 ===

    @Select("SELECT p.title AS title, p.view_count AS views, p.difficulty AS difficulty, p.category AS category " +
            "FROM interview_problem p WHERE p.status = 'ACTIVE' AND (p.is_deleted = 0 OR p.is_deleted IS NULL) " +
            "ORDER BY p.view_count DESC LIMIT 5")
    List<Map<String, Object>> getTopInterviewProblems();

    // === 昨日对比 ===

    @Select("SELECT COUNT(DISTINCT user_id) FROM login_log WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND login_time < CURDATE() AND status = 'success'")
    int countYesterdayActiveUsers();

    @Select("SELECT COUNT(*) FROM submission WHERE submit_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND submit_time < CURDATE()")
    int countYesterdaySubmissions();

    @Select("SELECT IFNULL(SUM(ai_dialogues), 0) FROM user WHERE last_visit_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND last_visit_time < CURDATE()")
    int countYesterdayAIDialogues();
}
