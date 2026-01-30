package com.uniops.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uniops.core.condition.TraceIdLogCondition;
import com.uniops.core.entity.LogInfo;
import com.uniops.core.vo.LogInfoVO;

import java.util.List;

/**
 * TraceIdLogService 接口
 * 用于根据traceId和时间范围检索日志文件（包括压缩文件）
 *
 * @author liyang
 * @since 2026/1/30
 */
public interface TraceIdLogService {
    /**
     * 根据traceId和时间范围检索日志文件（包括压缩文件），返回格式化日志
     */
    LogInfoVO getLogLinesByTraceId(TraceIdLogCondition condition);

    /**
     * 根据traceId和时间范围检索HTTP请求日志
     */
    LogInfoVO getHttpLogsByTraceId(TraceIdLogCondition condition);

    /**
     * 根据traceId和时间范围检索定时任务日志
     */
    LogInfoVO getScheduledLogsByTraceId(TraceIdLogCondition condition);

    /**
     * 获取完整的链路日志（从日志文件中，包括压缩文件）
     */
    LogInfoVO getFullTraceLog(String traceId, java.util.Date startTime, java.util.Date endTime, int page, int size);
}
