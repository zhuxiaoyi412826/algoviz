package com.algoviz.config;

import com.algoviz.mapper.DashboardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动阶段预热（避免首个用户请求遇到"冷启动 3s"问题）。
 *
 * 触发的预热内容：
 *   1. JDBC：触发一次 DB 查询 -> HikariCP 立刻建立最小连接数连接，MySQL 首条 TCP 握手
 *            在后台完成，用户无感。同时顺带触发 MyBatis 对 DashboardMapper 的首次注解 SQL 解析
 *            （否则第一次走 mapper 方法的 lazy init 也会叠加到首请求耗时里）。
 *   2. Lettuce/Redis：执行 PING -> 建立与 Redis 的首个 TCP 连接，完成 AUTH、ClientSetinfo、
 *      连接池初始化；Sa-Token 第一次 checkLogin 时就不用付出这个代价。
 *
 *  两项都失败不影响主进程启动（只打 warn），防止本地 MySQL/Redis 暂未就绪时应用启不来。
 */
@Component
public class StartupWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmupRunner.class);

    @Autowired(required = false)
    private DashboardMapper dashboardMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[WARMUP] start jdbc + redis connection pool ...");

        // ---------- 1. JDBC + MyBatis Mapper 预热 ----------
        long t0 = System.currentTimeMillis();
        try {
            if (dashboardMapper != null) {
                // 预热最重的 getUserStats()：扫 user_visit_stat 94万行（66MB），把数据加载进 InnoDB buffer pool。
                // 去掉 totalUsers 子查询后，此查询只碰 stat 表（66MB），预热后用户首次请求 <500ms。
                Map<String, Object> stats = dashboardMapper.getUserStats();
                Map<String, Object> sub = dashboardMapper.getSubmissionStats();
                int todayActive = dashboardMapper.countTodayActiveUsers();
                log.info("[WARMUP] jdbc ready users={} dsVisits={} submissions={} todayActive={} cost={}ms",
                        stats.get("totalUsers"), stats.get("dsVisits"),
                        sub.get("totalSubmissions"), todayActive,
                        System.currentTimeMillis() - t0);
            } else {
                log.warn("[WARMUP] DashboardMapper 未注入，跳过 JDBC 预热");
            }
        } catch (Exception e) {
            long c = System.currentTimeMillis() - t0;
            log.warn("[WARMUP] jdbc warmup failed after {}ms: {}", c, e.getMessage());
        }

        // ---------- 2. Redis 预热 ----------
        long t1 = System.currentTimeMillis();
        try {
            if (redisTemplate != null) {
                // StringRedisTemplate.opsForValue().get 会触发首次建立 lettuce 连接（AUTH + SELECT + CLIENT SETINFO），
                // 足以完成连接池预热；不需要真正拿到 PONG。
                try {
                    String dummy = redisTemplate.opsForValue().get("__warmup_probe__");
                    log.info("[WARMUP] redis ready cost={}ms (probe get={})", System.currentTimeMillis() - t1, dummy);
                } catch (Throwable ignored) {
                    // Redis 暂时不可用也没关系（启动失败也不影响应用）：尝试 conn.ping 做兜底连接
                    try {
                        Object pong = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) conn ->
                                conn.ping());
                        log.info("[WARMUP] redis fallback ping ok cost={}ms pong={}", System.currentTimeMillis() - t1, pong);
                    } catch (Throwable e2) {
                        throw new RuntimeException(e2);
                    }
                }
            } else {
                log.warn("[WARMUP] StringRedisTemplate 未注入，跳过 Redis 预热（Sa-Token/限流首次连接仍会冷）");
            }
        } catch (Exception e) {
            long c = System.currentTimeMillis() - t1;
            log.warn("[WARMUP] redis warmup failed after {}ms: {}", c, e.getMessage());
        }

        log.info("[WARMUP] done.");
    }
}
