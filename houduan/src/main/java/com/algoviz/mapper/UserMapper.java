package com.algoviz.mapper;

import com.algoviz.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    void insert(User user);
    void update(User user);
    void updateLastLoginAt(@Param("id") Integer id);
    
    @Select("SELECT * FROM user ORDER BY created_at DESC")
    List<User> getAllUsers();
    
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);
    
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);
    
    @Select("SELECT COUNT(*) FROM user")
    int countUsers();
}
