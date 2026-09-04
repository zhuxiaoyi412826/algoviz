package com.algoviz.service;

import com.algoviz.entity.UserVisitStat;

/**
 * 用户模块访问统计服务
 */
public interface UserVisitStatService {
    /**
     * 记录模块访问（数据库原子自增 + 刷新 last_visit_time）
     *
     * @param userId 用户ID
     * @param module 模块：ds / algo / oj / ai
     * @return 是否为合法模块
     */
    boolean recordVisit(Long userId, String module);

    /** 查询用户访问统计（无行返回全 0 默认对象） */
    UserVisitStat getStats(Long userId);

    /** 登录成功：写入最后登录时间 */
    void touchLogin(Long userId);

    /** 注册兜底：初始化 stat 行（幂等） */
    void initForUser(Long userId);
}
