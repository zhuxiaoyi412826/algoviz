package com.algoviz.service;

import com.algoviz.entity.Admin;
import java.util.List;
import java.util.Map;

public interface AdminService {
    Admin login(String username, String password, String ip);
    Admin getAdminInfo(String id);
    Map<String, Object> getAdminList(int page, int pageSize);
    Admin createAdmin(Admin admin);
    Admin updateAdmin(Admin admin);
    boolean deleteAdmin(String id);
    boolean resetPassword(String id);
    boolean changeStatus(String id, String status);
}
