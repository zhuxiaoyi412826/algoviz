package com.algoviz.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 向量同步进度持有者（全局内存状态）
 * 用于：全量同步向量库、批量导入题目后同步向量等长耗时任务
 * Vue 页面轮询 /sync/progress 端点获取此状态并渲染进度条
 */
@Component
public class SyncProgressHolder {

    /** 当前任务状态 */
    public enum Phase {
        /** 空闲中，没有正在进行的任务 */
        IDLE,
        /** 准备中（从 DB 拉题目、分组批处理） */
        PREPARING,
        /** 正在向量化 + 写入 */
        RUNNING,
        /** 完成 */
        COMPLETED,
        /** 失败 */
        FAILED
    }

    private final AtomicInteger processed = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong endTime = new AtomicLong(0);
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private volatile String message = "空闲中";
    private volatile String taskType = "none"; // vector_sync / es_sync
    private volatile Phase phase = Phase.IDLE;
    private volatile String lastError = null;

    // ==================== 公共只读访问 ====================

    /** 当前是否有任务在进行（供外部判断是否允许提交新任务） */
    public boolean isRunning() {
        return inProgress.get();
    }

    // ==================== 任务生命周期 ====================

    /** 开始一个新任务 */
    public synchronized void startTask(String type, int expectedTotal) {
        processed.set(0);
        failed.set(0);
        total.set(expectedTotal);
        startTime.set(System.currentTimeMillis());
        endTime.set(0);
        message = "任务已提交, 准备中...";
        phase = Phase.PREPARING;
        taskType = type;
        lastError = null;
        inProgress.set(true);
    }

    /** 进入运行阶段（题目全部读取完毕） */
    public void enterRunningPhase(int actualTotal) {
        total.set(actualTotal);
        phase = Phase.RUNNING;
        message = "向量化处理中, 共 " + actualTotal + " 条";
    }

    /** 单条已处理（成功） */
    public void incrementProcessed() {
        processed.incrementAndGet();
    }

    /** 批量已处理（成功） */
    public void addProcessed(int n) {
        processed.addAndGet(n);
    }

    /** 单条处理失败 */
    public void incrementFailed() {
        failed.incrementAndGet();
    }

    /** 批量失败 */
    public void addFailed(int n) {
        failed.addAndGet(n);
    }

    /** 更新临时状态消息（当前进度说明） */
    public void updateMessage(String msg) {
        this.message = msg;
    }

    /** 任务成功完成 */
    public void complete(String msg) {
        endTime.set(System.currentTimeMillis());
        message = msg == null ? "任务完成" : msg;
        phase = Phase.COMPLETED;
        inProgress.set(false);
    }

    /** 任务失败 */
    public void fail(String error) {
        endTime.set(System.currentTimeMillis());
        message = "任务失败: " + (error == null ? "未知错误" : error);
        lastError = error;
        phase = Phase.FAILED;
        inProgress.set(false);
    }

    /** 重置为空闲 */
    public void reset() {
        processed.set(0);
        failed.set(0);
        total.set(0);
        startTime.set(0);
        endTime.set(0);
        message = "空闲中";
        lastError = null;
        phase = Phase.IDLE;
        inProgress.set(false);
    }

    // ==================== 快照返回给前端 ====================

    /** 生成当前进度快照（不可变视图，前端直接序列化） */
    public ProgressSnapshot snapshot() {
        ProgressSnapshot s = new ProgressSnapshot();
        int curProcessed = processed.get();
        int curTotal = total.get();
        int curFailed = failed.get();
        long start = startTime.get();
        long end = endTime.get();

        s.setInProgress(inProgress.get());
        s.setPhase(phase.name().toLowerCase());
        s.setTaskType(taskType);
        s.setProcessed(curProcessed);
        s.setTotal(curTotal);
        s.setFailed(curFailed);
        s.setRemaining(curTotal > curProcessed ? curTotal - curProcessed : 0);
        s.setMessage(message);
        s.setLastError(lastError);

        // 进度百分比
        if (curTotal <= 0) {
            s.setPercent(0.0);
        } else {
            s.setPercent(Math.min(100.0, 100.0 * curProcessed / curTotal));
        }

        // 时间信息
        if (start > 0) {
            long now = (end > 0) ? end : System.currentTimeMillis();
            long elapsedMs = now - start;
            s.setStartTimeMs(start);
            s.setEndTimeMs(end > 0 ? end : null);
            s.setElapsedSeconds(elapsedMs / 1000);

            // 预计剩余时间：仅在运行阶段 & 有进度可算
            if (curProcessed > 0 && phase == Phase.RUNNING) {
                double perItemMs = (double) elapsedMs / curProcessed;
                long remainingMs = (long) (perItemMs * s.getRemaining());
                s.setEstimatedRemainingSeconds(remainingMs / 1000);
            } else if (end > 0) {
                s.setEstimatedRemainingSeconds(0L);
            } else {
                s.setEstimatedRemainingSeconds(null);
            }
        }

        return s;
    }

    @Data
    public static class ProgressSnapshot {
        /** 是否有任务在执行 */
        private boolean inProgress;
        /** idle / preparing / running / completed / failed */
        private String phase;
        /** vector_sync 或 es_sync */
        private String taskType;
        /** 已成功处理数 */
        private int processed;
        /** 失败数 */
        private int failed;
        /** 总任务数 */
        private int total;
        /** 剩余数 */
        private int remaining;
        /** 完成百分比 0.0 ~ 100.0 */
        private double percent;
        /** 人类可读状态消息 */
        private String message;
        /** 最后错误 */
        private String lastError;
        /** 开始时间戳 (ms) */
        private Long startTimeMs;
        /** 结束时间戳 (ms), 未结束时 null */
        private Long endTimeMs;
        /** 已用秒数 */
        private Long elapsedSeconds;
        /** 预计剩余秒数, 无法估算时 null */
        private Long estimatedRemainingSeconds;
    }
}
