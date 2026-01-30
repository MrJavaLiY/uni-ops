// src/main/java/com/uniops/core/service/impl/ThirdPartyHttpLogServiceImpl.java
package com.uniops.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniops.core.condition.ThirdPartyLogCondition;
import com.uniops.core.entity.ThirdPartyHttpLog;
import com.uniops.core.mapper.ThirdPartyHttpLogMapper;
import com.uniops.core.service.ISystemRegisterService;
import com.uniops.core.service.ThirdPartyHttpLogService;
import com.uniops.core.util.MDCUtil;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ThirdPartyHttpLogServiceImpl extends ServiceImpl<ThirdPartyHttpLogMapper, ThirdPartyHttpLog>
        implements ThirdPartyHttpLogService {

    @Autowired
    private ObjectMapper objectMapper;
    @Resource
    ISystemRegisterService systemRegisterService;

    @Override
    public boolean recordHttpCall(String thirdPartyName, String url, String method,
                                  Map<String, String> headers, String params, String body,
                                  Integer responseStatus, Map<String, String> responseHeaders,
                                  String responseBody, String errorMessage, Long duration) {
        ThirdPartyHttpLog thirdLog = new ThirdPartyHttpLog();
        thirdLog.setThirdPartyName(thirdPartyName);
        thirdLog.setUrl(url);
        thirdLog.setMethod(method);
        thirdLog.setRequestHeaders(mapToString(headers));
        thirdLog.setRequestParams(params);
        thirdLog.setRequestBody(body);
        thirdLog.setResponseStatus(responseStatus);
        thirdLog.setResponseHeaders(mapToString(responseHeaders));
        thirdLog.setResponseBody(responseBody);
        thirdLog.setErrorMessage(errorMessage);
        thirdLog.setDuration(duration);
        thirdLog.setRequestTime(LocalDateTime.now());
        thirdLog.setResponseTime(LocalDateTime.now());
        thirdLog.setCreatedAt(LocalDateTime.now());
        thirdLog.setUpdatedAt(LocalDateTime.now());
        thirdLog.setAppId(systemRegisterService.localSystem().getId());
        thirdLog.setLogTraceId(MDC.get(MDCUtil.TRACE_ID));
        return save(thirdLog);
    }

    @Override
    public IPage<ThirdPartyHttpLog> getLogsByThirdPartyName(Page<ThirdPartyHttpLog> page, String thirdPartyName) {
        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();
        if (thirdPartyName != null && !thirdPartyName.isEmpty()) {
            wrapper.eq("third_party_name", thirdPartyName);
        }
        wrapper.orderByDesc("request_time");
        return page(page, wrapper);
    }

    @Override
    public IPage<ThirdPartyHttpLog> getLogsByCondition(ThirdPartyLogCondition condition) {
        Page<ThirdPartyHttpLog> page = new Page<>(condition.getPage(), condition.getSize());
        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();
        if (condition.getThirdPartyName() != null && !condition.getThirdPartyName().isEmpty()) {
            wrapper.eq("third_party_name", condition.getThirdPartyName());
        }
        if (condition.getStartTime() != null) {
            wrapper.ge("request_time", condition.getStartTime());
        }
        if (condition.getEndTime() != null) {
            wrapper.le("request_time", condition.getEndTime());
        }
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        wrapper.orderByDesc("id");
        return this.page(page, wrapper);
    }

    @Override
    public IPage<ThirdPartyHttpLog> getLogsByTimeRange(Page<ThirdPartyHttpLog> page,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();

        wrapper.orderByDesc("request_time");
        return page(page, wrapper);
    }

    @Override
    public IPage<ThirdPartyHttpLog> getErrorLogs(Page<ThirdPartyHttpLog> page) {
        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("error_message")
                .and(qw -> qw.ne("error_message", "").or().isNotNull("error_message"));
        wrapper.orderByDesc("request_time");
        return page(page, wrapper);
    }

    @Override
    public boolean cleanExpiredLogs(LocalDateTime expireTime) {
        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();
        wrapper.le("request_time", expireTime);
        return remove(wrapper);
    }

    private String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return map.toString();
        }
    }
}
