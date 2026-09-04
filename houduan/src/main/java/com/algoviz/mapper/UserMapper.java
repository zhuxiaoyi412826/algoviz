package com.algoviz.mapper;

import com.algoviz.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    /** 注册查重专用：含已逻辑删除/注销账号（用户名/邮箱永久占用） */
    User findByUsernameIncludeDeleted(@Param("username") String username);
    User findByEmailIncludeDeleted(@Param("email") String email);
    void insert(User user);
    void update(User user);
    /** 最后登录时间已拆至 user_visit_stat（XML 实现为 upsert） */
    void updateLastLoginAt(@Param("id") Integer id);

    /**
     * 获取所有用户（仅导出用，百万级数据不建议直接调用）
     * 已优化为延迟关联：子查询走覆盖索引，再回表取数据
     * 仅导出未逻辑删除用户；LEFT JOIN stat 表填充 last_login_at（已从 user 表拆出）
     */
    @Select("SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "INNER JOIN (SELECT id FROM user WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT 10000) t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> getAllUsers();

    /**
     * 分页查询（延迟关联优化深翻页）
     * 子查询只扫描 id 列（走索引），再回表取完整行数据；仅未逻辑删除
     */
    @Select("SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "INNER JOIN (SELECT id FROM user WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}) t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> getUsersByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 关键词搜索分页（延迟关联 + 前缀匹配走索引）
     * LIKE 'keyword%' 前缀匹配可走 username/email 唯一索引，毫秒级；仅未逻辑删除
     */
    @Select("SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "INNER JOIN (" +
            "  SELECT id FROM user " +
            "  WHERE is_deleted = 0 AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%')) " +
            "  ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}" +
            ") t ON u.id = t.id " +
            "ORDER BY u.created_at DESC")
    List<User> searchUsersByPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM user WHERE is_deleted = 0 AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%'))")
    int searchUsersCount(@Param("keyword") String keyword);

    @Select("SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "WHERE u.id = #{id} AND u.is_deleted = 0")
    User findById(@Param("id") Integer id);

    @Select("SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "WHERE u.is_deleted = 0 AND (u.username LIKE CONCAT(#{keyword}, '%') OR u.email LIKE CONCAT(#{keyword}, '%')) ORDER BY u.created_at DESC")
    List<User> searchUsers(@Param("keyword") String keyword);

    /**
     * 逻辑删除：置 is_deleted=1 并强制下线（login_status=1），数据保留
     */
    @Update("UPDATE user SET is_deleted = 1, login_status = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    void deleteById(@Param("id") Integer id);

    @Update("UPDATE user SET status = #{status}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("UPDATE user SET coins = coins + #{delta}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateCoins(@Param("id") Integer id, @Param("delta") int delta);

    @Update("UPDATE user SET coins = #{coins}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int setCoins(@Param("id") Integer id, @Param("coins") int coins);

    @Update("UPDATE user SET password = #{password}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    /**
     * 更新登录状态：0=在线（登录成功/会话续期），1=离线（登出/会话销毁）
     */
    @Update("UPDATE user SET login_status = #{loginStatus}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateLoginStatus(@Param("id") Integer id, @Param("loginStatus") Integer loginStatus);

    /**
     * 注销账号：status=-1 并强制下线（数据保留，后台仍可见）
     */
    @Update("UPDATE user SET status = -1, login_status = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int cancelAccount(@Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM user WHERE is_deleted = 0")
    int countUsers();

    /**
     * 条件查询（延迟关联优化深翻页）
     * 子查询只扫描 id（覆盖索引），按筛选条件 + created_at 排序后取分页 id，
     * 再 INNER JOIN 回表取完整行数据，避免深翻页扫描大量全行数据
     * 注意：gender/loginStatus/status 均为 Integer，0 是合法值，OGNL 只判 null
     */
    @Select("<script>" +
            "SELECT u.*, s.last_login_at FROM user u " +
            "LEFT JOIN user_visit_stat s ON s.user_id = u.id " +
            "INNER JOIN (" +
            "  SELECT id FROM user WHERE is_deleted = 0" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%'))" +
            "  </if>" +
            "  <if test='gender != null'>" +
            "    AND gender = #{gender}" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "  <if test='loginStatus != null'>" +
            "    AND login_status = #{loginStatus}" +
            "  </if>" +
            "  ORDER BY created_at ${order} LIMIT #{offset}, #{pageSize}" +
            ") t ON u.id = t.id " +
            "ORDER BY u.created_at ${order}" +
            "</script>")
    List<User> getUsersByConditions(@Param("keyword") String keyword,
                                    @Param("gender") Integer gender,
                                    @Param("status") Integer status,
                                    @Param("loginStatus") Integer loginStatus,
                                    @Param("order") String order,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM user WHERE is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (username LIKE CONCAT(#{keyword}, '%') OR email LIKE CONCAT(#{keyword}, '%'))" +
            "</if>" +
            "<if test='gender != null'>" +
            " AND gender = #{gender}" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            "<if test='loginStatus != null'>" +
            " AND login_status = #{loginStatus}" +
            "</if>" +
            "</script>")
    int getUsersCountByConditions(@Param("keyword") String keyword,
                                  @Param("gender") Integer gender,
                                  @Param("status") Integer status,
                                  @Param("loginStatus") Integer loginStatus);
}
