package com.uniops.core.quartz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * DynamicScheduler 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Component
public class DynamicScheduler {

    @Autowired
    private TaskScheduler taskScheduler;
    // 存储所有调度任务的Future
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private ScheduledFuture<?> scheduledFuture;

    // 启动任务
    public void startTask(Runnable task, CronExpression cron) {
        // 先停止现有任务
        stopTask();

        // 根据Cron表达式创建调度
        CronTrigger trigger = new CronTrigger(cron.toString());
        scheduledFuture = taskScheduler.schedule(task, trigger);
        System.out.println("任务已启动，Cron: " + cron);
    }
    /**
     * 启动单个任务
     */
    public void startTask(String taskName, Runnable task, String cronExpression) {
        // 先停止已存在的任务
        stopTask(taskName);

        try {
            CronTrigger trigger = new CronTrigger(cronExpression);
            ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
            scheduledTasks.put(taskName, future);
            System.out.println("任务[" + taskName + "]已启动，Cron: " + cronExpression);
        } catch (Exception e) {
            System.err.println("启动任务[" + taskName + "]失败: " + e.getMessage());
        }
    }

    // 停止任务
    public void stopTask() {
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
            System.out.println("任务已停止");
        }
    }
    /**
     * 停止单个任务
     */
    public void stopTask(String taskName) {
        ScheduledFuture<?> future = scheduledTasks.get(taskName);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            scheduledTasks.remove(taskName);
            System.out.println("任务[" + taskName + "]已停止");
        }
    }
    // 任务逻辑
    public Runnable getTask() {
        return () -> {
            System.out.println("执行动态任务: " + LocalDateTime.now());
        };
    }
}

