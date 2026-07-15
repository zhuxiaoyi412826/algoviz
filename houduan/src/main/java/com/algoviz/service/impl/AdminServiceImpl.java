package com.algoviz.service.impl;

import com.algoviz.entity.Admin;
import com.algoviz.mapper.AdminMapper;
import com.algoviz.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public Admin login(String username, String password, String ip) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            return null;
        }

        // 简单的密码校验（实际项目应该使用BCrypt等加密）
        if ("admin123".equals(password) && "admin".equals(username)) {
            adminMapper.updateLastLogin(admin.getId(), ip);
            admin.setPassword(null); // 不返回密码
            return admin;
        }

        return null;
    }

    @Override
    public Admin getAdminInfo(String id) {
        Admin admin = adminMapper.findById(id);
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }

    @Override
    public Map<String, Object> getAdminList(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Admin> list = adminMapper.findByPage(offset, pageSize);
        list.forEach(admin -> admin.setPassword(null));

        int total = adminMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Override
    public Admin createAdmin(Admin admin) {
        admin.setId(UUID.randomUUID().toString());
        admin.setPassword("admin123"); // 默认密码
        admin.setStatus("active");
        adminMapper.insert(admin);
        return adminMapper.findById(admin.getId());
    }

    @Override
    public Admin updateAdmin(Admin admin) {
        adminMapper.update(admin);
        return adminMapper.findById(admin.getId());
    }

    @Override
    public boolean deleteAdmin(String id) {
        return adminMapper.deleteById(id) > 0;
    }

    @Override
    public boolean resetPassword(String id) {
        return adminMapper.updatePassword(id, "admin123") > 0;
    }

    @Override
    public boolean changeStatus(String id, String status) {
        return adminMapper.updateStatus(id, status) > 0;
    }
}
