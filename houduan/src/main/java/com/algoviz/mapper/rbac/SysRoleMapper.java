package com.algoviz.mapper.rbac;

import com.algoviz.entity.rbac.SysMenu;
import com.algoviz.entity.rbac.SysRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    @Select("SELECT * FROM sys_role WHERE id = #{id}")
    SysRole findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_role WHERE role_code = #{code}")
    SysRole findByCode(@Param("code") String code);

    @Select("SELECT * FROM sys_role WHERE status = 1 ORDER BY role_level, sort_order, id")
    List<SysRole> findAll();

    @Select("<script>" +
            "SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 " +
            "  AND m.menu_type IN (1,2) " +
            "ORDER BY m.parent_id, m.sort_order" +
            "</script>")
    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM sys_menu WHERE status = 1 AND menu_type IN (1,2) ORDER BY parent_id, sort_order")
    List<SysMenu> findAllMenus();
}
