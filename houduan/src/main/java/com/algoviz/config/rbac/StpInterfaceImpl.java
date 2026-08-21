package com.algoviz.config.rbac;

import cn.dev33.satoken.stp.StpInterface;
import com.algoviz.mapper.rbac.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 自定义权限加载实现
 * Sa-Token 通过此接口从数据库获取用户的角色和权限列表，
 * 用于 @SaCheckRole / @SaCheckPermission 注解校验。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 返回指定账号 id 所拥有的权限码集合
     *  - 如果是超级管理员（id=1），直接返回 ["*"] 表示拥有全部权限
     *  - 否则从 sys_user_role → sys_role_permission → sys_permission + sys_role_menu → sys_menu.perms 联合查询
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            Long userId = Long.valueOf(String.valueOf(loginId));
            // 超级管理员：拥有全部权限
            if (userId == 1L) {
                List<String> all = new ArrayList<>();
                all.add("*");
                return all;
            }
            List<String> perms = sysUserMapper.findPermissionCodesByUserId(userId);
            return perms == null ? new ArrayList<>() : perms;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 返回指定账号 id 所拥有的角色编码集合
     *  - 超级管理员（id=1）追加一个 ["SUPER_ADMIN"] 兜底
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = Long.valueOf(String.valueOf(loginId));
            List<String> roles = sysUserMapper.findRoleCodesByUserId(userId);
            if (roles == null) roles = new ArrayList<>();
            // 超级管理员兜底
            if (userId == 1L && !roles.contains("SUPER_ADMIN")) {
                roles.add("SUPER_ADMIN");
            }
            return roles;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
