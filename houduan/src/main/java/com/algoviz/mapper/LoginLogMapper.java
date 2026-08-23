package com.algoviz.mapper;

import com.algoviz.entity.LoginLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper {

    @Insert("INSERT INTO login_log (id, user_id, username, ip, device, location, login_time, status, fail_reason) " +
            "VALUES (#{id}, #{userId}, #{username}, #{ip}, #{device}, #{location}, #{loginTime}, #{status}, #{failReason})")
    int insert(LoginLog loginLog);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByUsername(@Param("offset") int offset, @Param("limit") int limit, @Param("username") String username);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE status = #{status} ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByStatus(@Param("offset") int offset, @Param("limit") int limit, @Param("status") String status);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND status = #{status} ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByUsernameAndStatus(@Param("offset") int offset, @Param("limit") int limit,
                                           @Param("username") String username, @Param("status") String status);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByDateRange(@Param("offset") int offset, @Param("limit") int limit,
                                  @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByUsernameAndDateRange(@Param("offset") int offset, @Param("limit") int limit,
                                              @Param("username") String username,
                                              @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE status = #{status} AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByStatusAndDateRange(@Param("offset") int offset, @Param("limit") int limit,
                                            @Param("status") String status,
                                            @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND status = #{status} AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) ORDER BY login_time DESC LIMIT #{offset}, #{limit}")
    List<LoginLog> findByAllFilters(@Param("offset") int offset, @Param("limit") int limit,
                                    @Param("username") String username, @Param("status") String status,
                                    @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM login_log")
    int count();

    @Select("SELECT COUNT(*) FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%')")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM login_log WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND status = #{status}")
    int countByUsernameAndStatus(@Param("username") String username, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM login_log WHERE login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)")
    int countByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)")
    int countByUsernameAndDateRange(@Param("username") String username,
                                    @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM login_log WHERE status = #{status} AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)")
    int countByStatusAndDateRange(@Param("status") String status,
                                  @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM login_log WHERE username LIKE CONCAT('%', #{username}, '%') AND status = #{status} AND login_time >= #{startDate} AND login_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)")
    int countByAllFilters(@Param("username") String username, @Param("status") String status,
                          @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM login_log WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    int countToday();

    @Select("SELECT COUNT(*) FROM login_log WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    int countWeek();

    @Select("SELECT COUNT(*) FROM login_log WHERE status = 'failed'")
    int countFailed();

    @Select("SELECT COUNT(*) FROM login_log WHERE status = 'success'")
    int countSuccess();

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log WHERE id = #{id}")
    LoginLog findById(@Param("id") String id);

    @Select("SELECT id, user_id AS userId, username, ip, device, location, login_time AS loginTime, status, fail_reason AS failReason FROM login_log ORDER BY login_time DESC")
    List<LoginLog> findAll();
}
