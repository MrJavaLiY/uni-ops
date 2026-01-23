// src/main/java/com/uniops/core/service/impl/HttpRequestLogServiceImpl.java
package com.uniops.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.annotation.NoManagedJob;
import com.uniops.core.condition.HttpLogRequestCondition;
import com.uniops.core.condition.SystemCondition;
import com.uniops.core.entity.HttpRequestLog;
import com.uniops.core.mapper.HttpRequestLogMapper;
import com.uniops.core.service.HttpRequestLogService;
import com.uniops.core.service.ISystemRegisterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class HttpRequestLogServiceImpl extends ServiceImpl<HttpRequestLogMapper, HttpRequestLog>
        implements HttpRequestLogService {

    @Resource
    SystemCondition systemCondition;
    @Resource
    ISystemRegisterService systemRegisterService;

    @Override
    public boolean saveLog(HttpRequestLog log) {
        return save(log);
    }


    @Override
    public IPage<HttpRequestLog> getLogsByCondition(HttpLogRequestCondition condition) {
        Page<HttpRequestLog> page = new Page<>(condition.getPage(), condition.getSize());
        page.addOrder(OrderItem.asc("id"));
        QueryWrapper<HttpRequestLog> wrapper = new QueryWrapper<>();
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        if (condition.getStartTime() != null) {
            wrapper.ge("request_time", condition.getStartTime());
        }
        if (condition.getEndTime() != null) {
            wrapper.le("request_time", condition.getEndTime());
        }
        if (condition.getApiPath() != null && !condition.getApiPath().isEmpty()) {
            wrapper.eq("api_path", condition.getApiPath());
        }
        if (condition.getHttpMethod() != null && !condition.getHttpMethod().isEmpty()) {
            wrapper.eq("http_method", condition.getHttpMethod());
        }
        if (condition.getStatusCode() != null && !condition.getStatusCode().isEmpty()) {
            wrapper.eq("status_code", condition.getStatusCode());
        }
        if (condition.getIp() != null && !condition.getIp().isEmpty()) {
            wrapper.eq("client_ip", condition.getIp());
        }
        return page(page, wrapper);
    }


    @Override
    public IPage<HttpRequestLog> getErrorLogs(Page<HttpRequestLog> page) {
        QueryWrapper<HttpRequestLog> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("exception_stack")
                .and(qw -> qw.ne("exception_stack", "").or().isNotNull("exception_stack"));
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        wrapper.orderByDesc("request_time");
        return page(page, wrapper);
    }

    @Override
    public boolean cleanExpiredLogs(LocalDateTime expireTime) {
        QueryWrapper<HttpRequestLog> wrapper = new QueryWrapper<>();
        wrapper.le("request_time", expireTime);
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        return remove(wrapper);
    }

    @Scheduled(cron = "0 0 1 * * *")
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
        cleanExpiredLogs(LocalDateTime.now().minusDays(7));
    }
}
