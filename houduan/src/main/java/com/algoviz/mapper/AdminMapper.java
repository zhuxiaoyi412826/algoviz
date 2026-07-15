package com.algoviz.mapper;

import com.algoviz.entity.Admin;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin findByUsername(String username);

    @Select("SELECT * FROM admin WHERE id = #{id}")
    Admin findById(String id);

    @Select("SELECT * FROM admin LIMIT #{limit} OFFSET #{offset}")
    List<Admin> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM admin")
    int count();

    @Insert("INSERT INTO admin (id, username, nickname, password, role, status, created_at, updated_at) " +
            "VALUES (#{id}, #{username}, #{nickname}, #{password}, #{role}, #{status}, datetime('now'), datetime('now'))")
    int insert(Admin admin);

    @Update("UPDATE admin SET nickname=#{nickname}, email=#{email}, phone=#{phone}, role=#{role}, status=#{status}, updated_at=datetime('now') WHERE id=#{id}")
    int update(Admin admin);

    @Delete("DELETE FROM admin WHERE id=#{id}")
    int deleteById(String id);

    @Update("UPDATE admin SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") String id, @Param("password") String password);

    @Update("UPDATE admin SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    @Update("UPDATE admin SET last_login_time=datetime('now'), last_login_ip=#{ip} WHERE id=#{id}")
    int updateLastLogin(@Param("id") String id, @Param("ip") String ip);
}
