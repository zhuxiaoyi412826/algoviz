package com.algoviz.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.sql.Date;

/**
 * 写时累加汇总表 Mapper（stat_total 单行全量 + stat_daily 自然日事件）
 *
 * 设计动机：把 Dashboard / 用户总数等「读时全表 COUNT/SUM」改为「写时累加」，
 * 读时仅 1~2 行主键查询。命中规则：
 *  - stat_total.total_users：有效用户(user.is_deleted=0)实时计数，注册 +1 / 后台删除 -1，每日校准兜底
 *  - stat_total.total_*：模块访问/AI 对话累计事件；用户被删除时按其在 stat 行的累计贡献扣减
 *  - stat_daily：自然日事件计数（ds/algo/oj/ai），只增不删，不做历史回填
 * 全部用 INSERT...ON DUPLICATE KEY UPDATE 原子自增，无读-改-写竞态
 */
@Mapper
public interface StatAccumulatorMapper {

    // ==================== stat_total：全量用户数 ====================

    /** 注册成功：有效用户 +1（INSERT 保证首次建行） */
    @Insert("INSERT INTO stat_total (id, total_users) VALUES (1, 1) " +
            "ON DUPLICATE KEY UPDATE total_users = total_users + 1")
    int incTotalUsers();

    /** 读取当前有效用户数（用户列表裸 COUNT 缓存源） */
    @Select("SELECT total_users FROM stat_total WHERE id = 1")
    Long getTotalUsers();

    // ==================== stat_total：模块累计事件 ====================

    @Insert("INSERT INTO stat_total (id, total_ds_visits) VALUES (1, 1) " +
            "ON DUPLICATE KEY UPDATE total_ds_visits = total_ds_visits + 1")
    int incTotalDs();

    @Insert("INSERT INTO stat_total (id, total_algo_visits) VALUES (1, 1) " +
            "ON DUPLICATE KEY UPDATE total_algo_visits = total_algo_visits + 1")
    int incTotalAlgo();

    @Insert("INSERT INTO stat_total (id, total_oj_visits) VALUES (1, 1) " +
            "ON DUPLICATE KEY UPDATE total_oj_visits = total_oj_visits + 1")
    int incTotalOj();

    @Insert("INSERT INTO stat_total (id, total_ai_dialogues) VALUES (1, 1) " +
            "ON DUPLICATE KEY UPDATE total_ai_dialogues = total_ai_dialogues + 1")
    int incTotalAi();

    /** 后台删除用户：total_users -1，并按该用户 stat 行累计贡献逐项扣减（与旧 WHERE is_deleted=0 口径一致） */
    @Update("UPDATE stat_total SET " +
            "total_users = GREATEST(total_users - 1, 0), " +
            "total_ds_visits = GREATEST(total_ds_visits - #{ds}, 0), " +
            "total_algo_visits = GREATEST(total_algo_visits - #{algo}, 0), " +
            "total_oj_visits = GREATEST(total_oj_visits - #{oj}, 0), " +
            "total_ai_dialogues = GREATEST(total_ai_dialogues - #{ai}, 0) " +
            "WHERE id = 1")
    int decByUserDelete(@Param("ds") long ds, @Param("algo") long algo,
                        @Param("oj") long oj, @Param("ai") long ai);

    /** 每日 03:10 校准 stat_total（防写时累加漂移；以 user_visit_stat is_deleted=0 为唯一真源） */
    @Update("UPDATE stat_total SET " +
            "total_users = (SELECT COUNT(*) FROM user_visit_stat WHERE is_deleted = 0), " +
            "total_ds_visits = (SELECT IFNULL(SUM(ds_visits), 0) FROM user_visit_stat WHERE is_deleted = 0), " +
            "total_algo_visits = (SELECT IFNULL(SUM(algo_visits), 0) FROM user_visit_stat WHERE is_deleted = 0), " +
            "total_oj_visits = (SELECT IFNULL(SUM(oj_visits), 0) FROM user_visit_stat WHERE is_deleted = 0), " +
            "total_ai_dialogues = (SELECT IFNULL(SUM(ai_dialogues), 0) FROM user_visit_stat WHERE is_deleted = 0), " +
            "updated_at = NOW() WHERE id = 1")
    int reconcileTotals();

    // ==================== stat_daily：自然日事件计数 ====================

    @Insert("INSERT INTO stat_daily (stat_date, ds_visits) VALUES (#{date}, 1) " +
            "ON DUPLICATE KEY UPDATE ds_visits = ds_visits + 1")
    int incDailyDs(@Param("date") Date date);

    @Insert("INSERT INTO stat_daily (stat_date, algo_visits) VALUES (#{date}, 1) " +
            "ON DUPLICATE KEY UPDATE algo_visits = algo_visits + 1")
    int incDailyAlgo(@Param("date") Date date);

    @Insert("INSERT INTO stat_daily (stat_date, oj_visits) VALUES (#{date}, 1) " +
            "ON DUPLICATE KEY UPDATE oj_visits = oj_visits + 1")
    int incDailyOj(@Param("date") Date date);

    @Insert("INSERT INTO stat_daily (stat_date, ai_dialogues) VALUES (#{date}, 1) " +
            "ON DUPLICATE KEY UPDATE ai_dialogues = ai_dialogues + 1")
    int incDailyAi(@Param("date") Date date);
}
