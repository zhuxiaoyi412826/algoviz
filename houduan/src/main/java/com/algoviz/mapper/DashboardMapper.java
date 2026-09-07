package com.algoviz.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /**
     * 合并查询1（写时累加改造）：不再全表 COUNT/SUM user_visit_stat，
     * 改为读 stat_total（单行）+ stat_daily（今/昨各 1 行）——O(1) 主键查询。
     * 返回字段名与旧实现完全一致：
     *   totalUsers, totalAIDialogues, todayAIDialogues, yesterdayAIDialogues,
     *   dsVisits, algoVisits, ojVisits
     * 口径说明：
     *   - totalUsers/totalAIDialogues/ds/algo/ojVisits = stat_total（注册+1、删除-1/扣贡献、每日校准）
     *   - todayAIDialogues/yesterdayAIDialogues = stat_daily 自然日真实事件（旧近似口径已废弃）
     */
    @Select("SELECT t.total_users AS totalUsers, " +
            "t.total_ai_dialogues AS totalAIDialogues, " +
            "t.total_ds_visits AS dsVisits, " +
            "t.total_algo_visits AS algoVisits, " +
            "t.total_oj_visits AS ojVisits, " +
            "IFNULL(d.ai_dialogues, 0) AS todayAIDialogues, " +
            "IFNULL(dy.ai_dialogues, 0) AS yesterdayAIDialogues " +
            "FROM stat_total t " +
            "LEFT JOIN stat_daily d  ON d.stat_date = CURDATE() " +
            "LEFT JOIN stat_daily dy ON dy.stat_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY) " +
            "WHERE t.id = 1")
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
