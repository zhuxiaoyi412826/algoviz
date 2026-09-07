package com.algoviz.task;

import com.algoviz.mapper.StatAccumulatorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 写时累加汇总表每日校准任务：
 * 写时累加在极端场景（脚本批量插删、DB 直改、异步补单）下可能与真源漂移，
 * 每天 03:10 以 user_visit_stat(is_deleted=0) 为唯一真源重算 stat_total 单行。
 * 注意：stat_daily 是自然日事件流水，只增不回滚，不参与校准。
 */
@Slf4j
@Component
public class StatReconcileTask {

    @Autowired
    private StatAccumulatorMapper statAccumulatorMapper;

    /** 每天 03:10 校准 stat_total（避开业务高峰） */
    @Scheduled(cron = "0 10 3 * * *")
    public void reconcileDaily() {
        try {
            int rows = statAccumulatorMapper.reconcileTotals();
            log.info("[stat] 每日校准 stat_total 完成，影响行数={}", rows);
        } catch (Exception e) {
            log.warn("[stat] 每日校准 stat_total 失败(次日重试): {}", e.getMessage());
        }
    }
}
