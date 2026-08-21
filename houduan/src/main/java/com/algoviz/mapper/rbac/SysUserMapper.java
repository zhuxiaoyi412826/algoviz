package com.algoviz.mapper.rbac;

import com.algoviz.entity.rbac.SysUser;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted = 0 LIMIT 1")
    SysUser findById(@Param("id") Long id);

    @Update("UPDATE sys_user SET last_login_time = #{time}, last_login_ip = #{ip} WHERE id = #{id}")
    int updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time, @Param("ip") String ip);

    @Insert("INSERT INTO sys_user(username, password, real_name, email, phone, avatar, status, account_type, created_by, created_at) " +
            "VALUES(#{username}, #{password}, #{realName}, #{email}, #{phone}, #{avatar}, #{status}, #{accountType}, #{createdBy}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Select("<script>" +
            "SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1" +
            "</script>")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 " +
            "UNION " +
            "SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.perms IS NOT NULL AND m.perms != ''" +
            "</script>")
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    int count();

    @Select("SELECT * FROM sys_user WHERE deleted = 0 ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<SysUser> findByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Update("<script>" +
            "UPDATE sys_user SET " +
            "  real_name = COALESCE(#{realName}, real_name), " +
            "  email = COALESCE(#{email}, email), " +
            "  phone = COALESCE(#{phone}, phone), " +
            "  avatar = COALESCE(#{avatar}, avatar), " +
            "  status = COALESCE(#{status}, status), " +
            "  account_type = COALESCE(#{accountType}, account_type), " +
            "  updated_at = NOW() " +
            "WHERE id = #{id}" +
            "</script>")
    int update(SysUser user);

    @Update("UPDATE sys_user SET deleted = 1 WHERE id = #{id} AND id != 1")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE sys_user SET password = #{password}, updated_at = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
