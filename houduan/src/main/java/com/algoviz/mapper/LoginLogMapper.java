package com.algoviz.mapper;

import com.algoviz.entity.LoginLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface LoginLogMapper {
    @Insert("INSERT INTO login_log (id, user_id, username, ip, device, location, login_time, status, fail_reason) " +
            "VALUES (#{id}, #{userId}, #{username}, #{ip}, #{device}, #{location}, #{loginTime}, #{status}, #{failReason})")
    int insert(LoginLog loginLog);

    @Select("SELECT * FROM login_log ORDER BY login_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<LoginLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM login_log")
    int count();

    @Select("SELECT COUNT(*) FROM login_log WHERE login_time >= date('now', '-1 day')")
    int countToday();

    @Select("SELECT COUNT(*) FROM login_log WHERE login_time >= date('now', '-7 day')")
    int countWeek();

    @Select("SELECT COUNT(*) FROM login_log WHERE status = 'failed'")
    int countFailed();

    @Select("SELECT * FROM login_log ORDER BY login_time DESC")
    List<LoginLog> findAll();
}
