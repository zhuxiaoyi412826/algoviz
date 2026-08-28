package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.AppUser;
import com.algoviz.entity.Statistics;
import com.algoviz.mapper.AppUserMapper;
import com.algoviz.mapper.StatisticsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "用户管理(后台)", description = "后台用户管理与统计")
public class AdminUserController {

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private StatisticsMapper statisticsMapper;

    @GetMapping("/admin/user/list")
    @Operation(summary = "查询用户列表", description = "分页查询后台用户列表")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AppUser> list = appUserMapper.findByPage(offset, pageSize);
        int total = appUserMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/user/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详情")
    public ApiResponse<AppUser> getUserById(@PathVariable String id) {
        return ApiResponse.success(appUserMapper.findById(id));
    }

    @PutMapping("/admin/user/{id}")
    @Operation(summary = "更新用户", description = "根据ID更新指定用户的资料信息")
    public ApiResponse<AppUser> updateUser(@PathVariable String id, @RequestBody AppUser user) {
        user.setId(id);
        appUserMapper.update(user);
        return ApiResponse.success(appUserMapper.findById(id));
    }

    @DeleteMapping("/admin/user/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        boolean success = appUserMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/statistics/trend")
    @Operation(summary = "查询统计趋势", description = "查询最近30天的访问统计趋势数据")
    public ApiResponse<Map<String, Object>> getStatisticsTrend() {
        List<Statistics> stats = statisticsMapper.findRecent(30);
        Map<String, Object> result = new HashMap<>();
        result.put("dates", stats.stream().map(Statistics::getDate).toList());
        result.put("dau", stats.stream().map(Statistics::getDau).toList());
        result.put("dsVisits", stats.stream().map(Statistics::getDsVisits).toList());
        result.put("algoVisits", stats.stream().map(Statistics::getAlgoVisits).toList());
        result.put("ojSubmissions", stats.stream().map(Statistics::getOjSubmissions).toList());
        result.put("aiDialogues", stats.stream().map(Statistics::getAiDialogues).toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics/summary")
    @Operation(summary = "查询统计摘要", description = "查询日活、周活、月活及用户总数等统计摘要")
    public ApiResponse<Map<String, Object>> getStatisticsSummary() {
        Map<String, Object> result = new HashMap<>();
        result.put("dau", statisticsMapper.countToday());
        result.put("wau", statisticsMapper.sumDauWeek());
        result.put("mau", statisticsMapper.sumDauMonth());
        result.put("totalUsers", appUserMapper.count());
        result.put("totalSubmissions", 0);
        result.put("totalDialogues", 0);
        return ApiResponse.success(result);
    }
}
