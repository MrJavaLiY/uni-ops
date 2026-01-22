package com.uniops.core.monitor;

import com.uniops.core.condition.SystemCondition;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.ScheduledLog;
import com.uniops.core.service.IScheduledConfigService;
import com.uniops.core.service.IScheduledLogService;

import java.util.Date;

/**
 * @Scheduled方法执行监控切面 拦截所有带@Scheduled注解的方法，记录执行日志
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class ScheduledMonitorAspect {

    @Autowired
    private IScheduledLogService logService;

    @Autowired
    private IScheduledConfigService configService;
    @Resource
    SystemCondition systemCondition;

    /**
     * 环绕通知：拦截@Scheduled方法
     */
    @Around("@annotation(scheduled)")
    public Object monitorScheduledMethod(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
        String beanName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 检查是否启用监控
        ScheduledConfig config = configService.getByBeanAndMethod(beanName, methodName);
        if (config == null || !"ENABLED".equals(config.getMonitorStatus())) {
            // 未启用监控，直接执行
            return joinPoint.proceed();
        }

        log.info("[UniOps] @Scheduled方法执行: {}.{}", beanName, methodName);

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String exceptionMsg = null;

        // 记录运行中状态
        ScheduledLog logEntry = new ScheduledLog();
        logEntry.setBeanName(beanName);
        logEntry.setMethodName(methodName);
        logEntry.setTriggerTime(new Date());
        logEntry.setStatus("RUNNING");
        logEntry.setTriggerType("SCHEDULED");
        logEntry.setAppName(systemCondition.getApplicationName());
        logService.save(logEntry);
        try {
            // 执行原方法
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            exceptionMsg = e.getMessage();
            throw e;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 更新日志状态
            logEntry.setStatus(success ? "SUCCESS" : "FAILED");
            logEntry.setDurationMs((int) duration);
            logEntry.setExceptionMsg(exceptionMsg);
            logEntry.setTriggerTime(new Date());
            logService.updateById(logEntry);

            // 更新配置中的最后执行时间
            config.setLastFireTime(new Date());
            configService.updateById(config);

            if (success) {
                log.info("[UniOps] @Scheduled方法执行成功: {}.{} (耗时:{}ms)", beanName, methodName, duration);
            } else {
                log.error("[UniOps] @Scheduled方法执行失败: {}.{} - {}", beanName, methodName, exceptionMsg);
            }
        }
    }

    /**
     * 更新下次执行时间（估算）
     */
    private void updateNextFireTime(ScheduledConfig config, Scheduled scheduled) {
        try {
            // 简单估算：根据Cron表达式计算下次执行时间
            // 实际生产环境可以使用Cron表达式解析库
            if (!scheduled.cron().isEmpty()) {
                // 这里简化处理，实际应该解析Cron表达式
                Date nextFire = new Date(System.currentTimeMillis() + 60000); // 1分钟后
                config.setNextFireTime(nextFire);
                configService.updateById(config);
            }
        } catch (Exception e) {
            log.warn("更新下次执行时间失败", e);
        }
    }
}
