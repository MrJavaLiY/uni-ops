// src/main/java/com/uniops/core/service/ThirdPartyHttpLogService.java
package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.condition.ThirdPartyLogCondition;
import com.uniops.core.entity.ThirdPartyHttpLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.Map;

public interface ThirdPartyHttpLogService extends IService<ThirdPartyHttpLog> {

    /**
     * 记录第三方HTTP调用日志
     */
    boolean recordHttpCall(String thirdPartyName, String url, String method,
                          Map<String, String> headers, String params, String body,
                          Integer responseStatus, Map<String, String> responseHeaders,
                          String responseBody, String errorMessage, Long duration);

    /**
     * 根据第三方服务名称查询日志
     */
    IPage<ThirdPartyHttpLog> getLogsByThirdPartyName(Page<ThirdPartyHttpLog> page, String thirdPartyName);
    /**
     * 根据第三方服务名称查询日志
     */
    IPage<ThirdPartyHttpLog> getLogsByCondition(ThirdPartyLogCondition condition);

    /**
     * 根据时间范围查询日志
     */
    IPage<ThirdPartyHttpLog> getLogsByTimeRange(Page<ThirdPartyHttpLog> page,
                                               LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询错误日志
     */
    IPage<ThirdPartyHttpLog> getErrorLogs(Page<ThirdPartyHttpLog> page);

    /**
     * 清理过期日志
     */
    boolean cleanExpiredLogs(LocalDateTime expireTime);
}
