package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.annotation.NoManagedJob;
import com.uniops.core.condition.ScheduledRequestCondition;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.ScheduledLog;
import com.uniops.core.mapper.ScheduledLogMapper;
import com.uniops.core.service.IScheduledLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ScheduledLogServiceImpl extends ServiceImpl<ScheduledLogMapper, ScheduledLog>
        implements IScheduledLogService {

    @Resource
    IScheduledConfigService scheduledConfigService;

    @Override
    public Page<ScheduledLog> getLogs(ScheduledRequestCondition condition) {
        ScheduledConfig config = scheduledConfigService.getById(condition.getId());
        return this.page(
                new Page<ScheduledLog>(condition.getPage(), condition.getSize()),
                new LambdaQueryWrapper<ScheduledLog>()
                        .eq(ScheduledLog::getBeanName, config.getBeanName())
                        .eq(ScheduledLog::getMethodName, config.getMethodName())
                        .eq(ScheduledLog::getAppName, config.getAppName())
                        .orderByDesc(ScheduledLog::getTriggerTime)
        );
    }

    @Override
    public List<ScheduledLog> getRecentFailures(int limit) {
        return this.list(
                new LambdaQueryWrapper<ScheduledLog>()
                        .eq(ScheduledLog::getStatus, "FAILED")
                        .orderByDesc(ScheduledLog::getTriggerTime)
                        .last("LIMIT " + limit)
        );
    }

    @Scheduled(cron = "0 0 0 * * *")
    @NoManagedJob
    public void clearLogs() {
        //清理表里面的日志数据，根据createat保留7天
        //等待一个一小时内的随机时间，然后再执行，避免多个服务之间相互锁表
        long randomTime = (long) (Math.random() * 3600 * 1000);
        try {
            Thread.sleep(randomTime);
        } catch (InterruptedException e) {
            log.warn("[UniOps] clearLogs sleep error,", e);
        }
        this.remove(new LambdaQueryWrapper<ScheduledLog>()
                .lt(ScheduledLog::getTriggerTime, System.currentTimeMillis() - 7 * 24 * 3600 * 1000));

    }
}