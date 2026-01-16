package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.entity.ScheduledLog;

import java.util.List;

/**
 * IScheduledLogService 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
public interface IScheduledLogService extends IService<ScheduledLog> {
    Page<ScheduledLog> getLogs(String beanName, String methodName, int page, int size);
    List<ScheduledLog> getRecentFailures(int limit);
}
