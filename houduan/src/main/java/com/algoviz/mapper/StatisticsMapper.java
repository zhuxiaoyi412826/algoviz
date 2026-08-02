package com.algoviz.mapper;

import com.algoviz.entity.Statistics;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StatisticsMapper {
    @Insert("INSERT OR REPLACE INTO statistics (id, date, dau, wau, mau, ds_visits, algo_visits, oj_submissions, oj_ac_rate, ai_dialogues, created_at) " +
            "VALUES (#{id}, #{date}, #{dau}, #{wau}, #{mau}, #{dsVisits}, #{algoVisits}, #{ojSubmissions}, #{ojAcRate}, #{aiDialogues}, NOW())")
    int insertOrUpdate(Statistics statistics);

    @Select("SELECT * FROM statistics ORDER BY date DESC LIMIT #{limit}")
    List<Statistics> findRecent(int limit);

    @Select("SELECT * FROM statistics WHERE date = #{date}")
    Statistics findByDate(String date);

    @Select("SELECT SUM(dau) as total FROM statistics WHERE date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    Integer sumDauWeek();

    @Select("SELECT SUM(dau) as total FROM statistics WHERE date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
    Integer sumDauMonth();

    @Select("SELECT COUNT(*) FROM statistics")
    int count();

    @Select("SELECT IFNULL(SUM(dau), 0) as total FROM statistics WHERE date = CURDATE()")
    Integer countToday();
}
