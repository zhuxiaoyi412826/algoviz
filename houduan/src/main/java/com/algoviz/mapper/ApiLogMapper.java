package com.algoviz.mapper;

import com.algoviz.entity.ApiLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ApiLogMapper {
    @Insert("INSERT INTO api_log (id, api_path, http_method, status_code, response_time, client_ip, request_body, response_body, error_message, user_id, create_time) " +
            "VALUES (#{id}, #{apiPath}, #{httpMethod}, #{statusCode}, #{responseTime}, #{clientIp}, #{requestBody}, #{responseBody}, #{errorMessage}, #{userId}, #{createTime})")
    int insert(ApiLog apiLog);

    @Select("SELECT * FROM api_log ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<ApiLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM api_log")
    int count();

    @Select("SELECT * FROM api_log WHERE id = #{id}")
    ApiLog findById(String id);

    @Select("SELECT api_path, COUNT(*) as total, SUM(response_time) as total_time, AVG(response_time) as avg_time, " +
            "MIN(response_time) as min_time, MAX(response_time) as max_time, " +
            "SUM(CASE WHEN status_code >= 200 AND status_code < 300 THEN 1 ELSE 0 END) as success_count " +
            "FROM api_log GROUP BY api_path ORDER BY total DESC LIMIT #{limit}")
    List<ApiStatistics> getApiStatistics(int limit);

    @Select("SELECT COUNT(*) FROM api_log WHERE date(create_time) = date('now')")
    int countToday();

    @Select("SELECT IFNULL(AVG(response_time), 0) FROM api_log WHERE date(create_time) = date('now')")
    Long avgResponseTimeToday();

    @Select("SELECT COUNT(*) FROM api_log WHERE status_code >= 400 AND date(create_time) = date('now')")
    int countErrorToday();

    @Select("SELECT strftime('%H', create_time) as hour, COUNT(*) as count FROM api_log WHERE date(create_time) = date('now') GROUP BY strftime('%H', create_time) ORDER BY hour")
    List<ApiHourlyStats> getHourlyStatsToday();

    @Select("SELECT strftime('%Y-%m-%d', create_time) as date, COUNT(*) as count, AVG(response_time) as avg_time FROM api_log GROUP BY date(create_time) ORDER BY date DESC LIMIT #{limit}")
    List<ApiDailyStats> getDailyStats(int limit);

    public interface ApiStatistics {
        String getApiPath();
        Integer getTotal();
        Long getTotalTime();
        Double getAvgTime();
        Long getMinTime();
        Long getMaxTime();
        Integer getSuccessCount();
    }

    public interface ApiHourlyStats {
        String getHour();
        Integer getCount();
    }

    public interface ApiDailyStats {
        String getDate();
        Integer getCount();
        Double getAvgTime();
    }

    @Delete("DELETE FROM api_log WHERE date(create_time) < date('now', '-30 day')")
    int cleanOldLogs();
}
