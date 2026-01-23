// src/main/java/com/uniops/core/service/HttpRequestLogService.java
package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.condition.HttpLogRequestCondition;
import com.uniops.core.entity.HttpRequestLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface HttpRequestLogService extends IService<HttpRequestLog> {

    /**
     * 保存请求日志
     */
    boolean saveLog(HttpRequestLog log);


    IPage<HttpRequestLog> getLogsByCondition(HttpLogRequestCondition condition);

    /**
     * 获取异常日志
     */
    IPage<HttpRequestLog> getErrorLogs(Page<HttpRequestLog> page);


    /**
     * 清理过期日志
     */
    boolean cleanExpiredLogs(LocalDateTime expireTime);
}
