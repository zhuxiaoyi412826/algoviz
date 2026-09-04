package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.entity.User;
import com.algoviz.entity.UserVisitStat;
import com.algoviz.service.UserVisitStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块访问统计
 * /api/visit/** 未在 WebConfig 排除列表，由 AuthInterceptor 统一登录保护
 * （Session / Cookie / X-User-Id 三种认证均可，拦截器保证 SESSION_USER 必然存在）
 */
@RestController
@RequestMapping("/api/visit")
@Tag(name = "模块访问统计", description = "用户模块访问上报与查询（user_visit_stat 拆表）")
public class VisitStatController {

    @Autowired
    private UserVisitStatService userVisitStatService;

    /** 从认证上下文取当前用户（AuthInterceptor 已保证登录） */
    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (attr instanceof User) {
                return (User) attr;
            }
        }
        return null;
    }

    @PostMapping("/{module}")
    @Operation(summary = "模块访问上报", description = "module: ds=数据结构 algo=算法 oj=OJ ai=AI助手；数据库原子自增计数并刷新最后访问时间")
    public Map<String, Object> recordVisit(@PathVariable String module, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        User user = currentUser(request);
        if (user == null || user.getId() == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }

        boolean ok = userVisitStatService.recordVisit(user.getId().longValue(), module);
        if (!ok) {
            result.put("success", false);
            result.put("message", "无效的模块: " + module + "（可选 ds/algo/oj/ai）");
            return result;
        }

        UserVisitStat stat = userVisitStatService.getStats(user.getId().longValue());
        result.put("success", true);
        result.put("message", "访问已记录");
        result.put("data", statData(stat));
        return result;
    }

    @GetMapping("/me")
    @Operation(summary = "查询本人访问统计", description = "返回四模块计数 + 最后登录/访问时间")
    public Map<String, Object> myStats(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        User user = currentUser(request);
        if (user == null || user.getId() == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        UserVisitStat stat = userVisitStatService.getStats(user.getId().longValue());
        result.put("success", true);
        result.put("data", statData(stat));
        return result;
    }

    private Map<String, Object> statData(UserVisitStat stat) {
        Map<String, Object> data = new HashMap<>();
        data.put("dsVisits", stat.getDsVisits());
        data.put("algoVisits", stat.getAlgoVisits());
        data.put("ojVisits", stat.getOjVisits());
        data.put("aiDialogues", stat.getAiDialogues());
        data.put("lastLoginAt", stat.getLastLoginAt() != null ? stat.getLastLoginAt().toString().replace("T", " ").substring(0, Math.min(19, stat.getLastLoginAt().toString().length())) : null);
        data.put("lastVisitTime", stat.getLastVisitTime() != null ? stat.getLastVisitTime().toString().replace("T", " ").substring(0, Math.min(19, stat.getLastVisitTime().toString().length())) : null);
        return data;
    }
}
