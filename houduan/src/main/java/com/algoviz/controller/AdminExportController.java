package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.AppUser;
import com.algoviz.entity.LoginLog;
import com.algoviz.mapper.AppUserMapper;
import com.algoviz.mapper.LoginLogMapper;
import com.algoviz.util.ExportUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "数据导出", description = "用户/日志数据导出")
public class AdminExportController {

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @GetMapping("/export/users")
    public void exportUsers(@RequestParam(defaultValue = "excel") String format,
                            HttpServletResponse response) throws IOException {
        List<AppUser> users = appUserMapper.findAll();
        List<String> headers = List.of("ID", "OpenID", "昵称", "头像", "绑定时间", "状态");
        List<List<Object>> data = users.stream().map(u -> {
            List<Object> row = new ArrayList<>();
            row.add(u.getId());
            row.add(u.getOpenid());
            row.add(u.getNickname());
            row.add(u.getAvatar());
            row.add(u.getBindTime());
            row.add(u.getStatus());
            return row;
        }).toList();

        exportData(format, "users", headers, data, response);
    }

    @GetMapping("/export/logs")
    public void exportLogs(@RequestParam(defaultValue = "excel") String format,
                           HttpServletResponse response) throws IOException {
        List<LoginLog> logs = loginLogMapper.findAll();
        List<String> headers = List.of("ID", "用户ID", "用户名", "IP", "设备", "登录时间", "状态", "失败原因");
        List<List<Object>> data = logs.stream().map(l -> {
            List<Object> row = new ArrayList<>();
            row.add(l.getId());
            row.add(l.getUserId());
            row.add(l.getUsername());
            row.add(l.getIp());
            row.add(l.getDevice());
            row.add(l.getLoginTime());
            row.add(l.getStatus());
            row.add(l.getFailReason());
            return row;
        }).toList();

        exportData(format, "login_logs", headers, data, response);
    }

    private void exportData(String format, String filename, List<String> headers,
                           List<List<Object>> data, HttpServletResponse response) throws IOException {
        byte[] content;
        String contentType;
        String extension;

        switch (format.toLowerCase()) {
            case "csv":
                content = ExportUtil.exportToCsv(headers, data);
                contentType = "text/csv";
                extension = ".csv";
                break;
            case "json":
                content = ExportUtil.exportToJson(headers, data);
                contentType = "application/json";
                extension = ".json";
                break;
            case "excel":
            default:
                content = ExportUtil.exportToExcel(headers, data);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
                break;
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" + filename + extension);
        response.getOutputStream().write(content);
    }
}