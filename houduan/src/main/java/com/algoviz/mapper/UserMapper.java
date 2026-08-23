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

    /**
     * 获取所有用户（仅导出用，百万级数据不建议直接调用）
     * 已优化为延迟关联：子查询走覆盖索引，再回表取数据
     */
    @Select("SELECT u.* FROM user u " +
            "INNER JOIN (SELECT id FROM user ORDER BY created_at DESC LIMIT 10000) t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> getAllUsers();

    /**
     * 分页查询（延迟关联优化深翻页）
     * 子查询只扫描 id 列（走 idx_user_created_at 索引），再回表取完整行数据
     */
    @Select("SELECT u.* FROM user u " +
            "INNER JOIN (SELECT id FROM user ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}) t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> getUsersByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 关键词搜索分页（延迟关联 + 前缀匹配走索引）
     * LIKE 'keyword%' 前缀匹配可走 username/email 唯一索引，毫秒级
     */
    @Select("SELECT u.* FROM user u " +
            "INNER JOIN (" +
            "  SELECT id FROM user " +
            "  WHERE username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%') " +
            "  ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}" +
            ") t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> searchUsersByPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM user WHERE username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%')")
    int searchUsersCount(@Param("keyword") String keyword);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("SELECT * FROM user WHERE username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%') ORDER BY created_at DESC")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);

    @Update("UPDATE user SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("UPDATE user SET coins = coins + #{delta}, updated_at = NOW() WHERE id = #{id}")
    int updateCoins(@Param("id") Integer id, @Param("delta") int delta);

    @Update("UPDATE user SET coins = #{coins}, updated_at = NOW() WHERE id = #{id}")
    int setCoins(@Param("id") Integer id, @Param("coins") int coins);

    @Update("UPDATE user SET password = #{password}, updated_at = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    @Select("SELECT COUNT(*) FROM user")
    int countUsers();

    /**
     * 条件查询（延迟关联优化深翻页）
     * 子查询只扫描 id（覆盖索引），按筛选条件 + created_at 排序后取分页 id，
     * 再 INNER JOIN 回表取完整行数据，避免深翻页扫描大量全行数据
     */
    @Select("<script>" +
            "SELECT u.* FROM user u " +
            "INNER JOIN (" +
            "  SELECT id FROM user WHERE 1=1" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%'))" +
            "  </if>" +
            "  <if test='gender != null and gender != \"\"'>" +
            "    AND gender = #{gender}" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "  <if test='loginStatus != null and loginStatus != \"\"'>" +
            "    AND login_status = #{loginStatus}" +
            "  </if>" +
            "  ORDER BY created_at ${order} LIMIT #{offset}, #{pageSize}" +
            ") t ON u.id = t.id " +
            "ORDER BY u.created_at ${order}" +
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
            " AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%'))" +
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
