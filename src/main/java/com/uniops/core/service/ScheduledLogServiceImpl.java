package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.entity.ScheduledLog;
import com.uniops.core.mapper.ScheduledLogMapper;
import com.uniops.core.service.IScheduledLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledLogServiceImpl extends ServiceImpl<ScheduledLogMapper, ScheduledLog>
        implements IScheduledLogService {

    @Override
    public Page<ScheduledLog> getLogs(String beanName, String methodName, int page, int size) {
        return this.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<ScheduledLog>()
                        .eq(beanName != null, ScheduledLog::getBeanName, beanName)
                        .eq(methodName != null, ScheduledLog::getMethodName, methodName)
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
}