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

    @Select("SELECT * FROM user ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<User> getUsersByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<User> searchUsersByPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')")
    int searchUsersCount(@Param("keyword") String keyword);
    
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);
    
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);

    @Update("UPDATE user SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("UPDATE user SET coins = coins + #{delta}, updated_at = NOW() WHERE id = #{id}")
    int updateCoins(@Param("id") Integer id, @Param("delta") int delta);

    @Update("UPDATE user SET coins = #{coins}, updated_at = NOW() WHERE id = #{id}")
    int setCoins(@Param("id") Integer id, @Param("coins") int coins);
    
    @Select("SELECT COUNT(*) FROM user")
    int countUsers();

    @Select("<script>" +
            "SELECT * FROM user WHERE 1=1" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='gender != null and gender != \"\"'>" +
            " AND gender = #{gender}" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            "<if test='loginStatus != null and loginStatus != \"\"'>" +
            " AND login_status = #{loginStatus}" +
            "</if>" +
            " ORDER BY created_at ${order} LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<User> getUsersByConditions(@Param("keyword") String keyword,
                                    @Param("gender") String gender,
                                    @Param("status") Integer status,
                                    @Param("loginStatus") String loginStatus,
                                    @Param("order") String order,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM user WHERE 1=1" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='gender != null and gender != \"\"'>" +
            " AND gender = #{gender}" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            "<if test='loginStatus != null and loginStatus != \"\"'>" +
            " AND login_status = #{loginStatus}" +
            "</if>" +
            "</script>")
    int getUsersCountByConditions(@Param("keyword") String keyword,
                                  @Param("gender") String gender,
                                  @Param("status") Integer status,
                                  @Param("loginStatus") String loginStatus);
}
