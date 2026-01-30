package com.uniops.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * LogInfo 实体类
 * 用于表示格式化的日志信息
 *
 * @author liyang
 * @since 2026/1/30
 */
@Data
public class LogInfo {
    /**
     * 日志等级 (INFO, DEBUG, WARN, ERROR等)
     */
    private String level;

    /**
     * 日志发生时间
     */
    private LocalDateTime logTime;

    /**
     * 日志详情
     */
    private String detail;

    /**
     * 日志所属的traceId
     */
    private String traceId;

    public LogInfo() {}

    public LogInfo(String level, LocalDateTime logTime, String detail, String traceId) {
        this.level = level;
        this.logTime = logTime;
        this.detail = detail;
        this.traceId = traceId;
    }
}
