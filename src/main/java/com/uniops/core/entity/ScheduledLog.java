package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.uniops.core.annotation.ManageEntity;
import lombok.Data;

import java.util.Date;

/**
 * Scheduled任务执行日志实体
 * 记录@Scheduled方法每次执行的详细信息
 */
@Data
@TableName("uniops_scheduled_log")
@ManageEntity(value = "uniops_scheduled_log", tableName = "uniops_scheduled_log", primaryKey = "id")
public class ScheduledLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bean_name")
    private String beanName;  // Bean名称
    @TableField("method_name")
    private String methodName;  // 方法名

    @TableField("app_name")
    private String appName;

    @TableField("trigger_time")
    private Date triggerTime;  // 执行时间

    @TableField("status")
    private String status;  // SUCCESS/FAILED/RUNNING

    @TableField("duration_ms")
    private Integer durationMs;  // 执行耗时（毫秒）

    @TableField("exception_msg")
    private String exceptionMsg;  // 异常信息

    @TableField("trigger_type")
    private String triggerType;  // SCHEDULED/MANUAL
    @TableField("log_trace_id")
    private String logTraceId;  // 日志链路追踪Id

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
