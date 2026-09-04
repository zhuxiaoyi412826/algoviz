package com.algoviz.mapper;

import com.algoviz.entity.UserVisitStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户模块访问统计 Mapper
 * 计数全部使用 INSERT ... ON DUPLICATE KEY UPDATE 单条 SQL 原子自增，无读-改-写竞态
 */
@Mapper
public interface UserVisitStatMapper {

    @Insert("INSERT INTO user_visit_stat (user_id, ds_visits, last_visit_time) VALUES (#{userId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE ds_visits = ds_visits + 1, last_visit_time = NOW()")
    int recordDsVisit(@Param("userId") Long userId);

    @Insert("INSERT INTO user_visit_stat (user_id, algo_visits, last_visit_time) VALUES (#{userId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE algo_visits = algo_visits + 1, last_visit_time = NOW()")
    int recordAlgoVisit(@Param("userId") Long userId);

    @Insert("INSERT INTO user_visit_stat (user_id, oj_visits, last_visit_time) VALUES (#{userId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE oj_visits = oj_visits + 1, last_visit_time = NOW()")
    int recordOjVisit(@Param("userId") Long userId);

    @Insert("INSERT INTO user_visit_stat (user_id, ai_dialogues, last_visit_time) VALUES (#{userId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE ai_dialogues = ai_dialogues + 1, last_visit_time = NOW()")
    int recordAiVisit(@Param("userId") Long userId);

    /** 登录成功：写入最后登录时间（无行自动建行） */
    @Insert("INSERT INTO user_visit_stat (user_id, last_login_at) VALUES (#{userId}, CURRENT_TIMESTAMP) " +
            "ON DUPLICATE KEY UPDATE last_login_at = CURRENT_TIMESTAMP")
    int touchLogin(@Param("userId") Long userId);

    /** 注册兜底：初始化 stat 行（幂等） */
    @Insert("INSERT IGNORE INTO user_visit_stat (user_id) VALUES (#{userId})")
    void initForUser(@Param("userId") Long userId);

    @Select("SELECT * FROM user_visit_stat WHERE user_id = #{userId}")
    UserVisitStat findByUserId(@Param("userId") Long userId);
}
