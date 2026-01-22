package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.uniops.core.annotation.CacheableEntity;
import lombok.Data;
import org.springframework.boot.autoconfigure.cache.CacheType;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * Scheduled任务配置实体
 * 用于存储@Scheduled注解方法的配置信息
 */
@Data
@TableName("uniops_scheduled_config")
@CacheableEntity(value = "scheduled_config", tableName = "uniops_scheduled_config", primaryKey = "id")
public class ScheduledConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bean_name")
    private String beanName;  // Bean名称（如：businessService）

    @TableField("method_name")
    private String methodName;  // 方法名（如：syncData）

    @TableField("app_name")
    private String appName;

    @TableField("cron_expression")
    private String cronExpression;  // Cron表达式（如：0/30 * * * * ?）

    @TableField("monitor_status")
    private String monitorStatus;  //状态：ENABLED/DISABLED

    @TableField("description")
    private String description;  // 描述

    @TableField("last_fire_time")
    private Date lastFireTime;  // 最后执行时间

    @TableField("next_fire_time")
    private Date nextFireTime;  // 下次执行时间

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;  // 创建时间

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;  // 更新时间

    @TableField(value = "enabled")
    private Boolean enabled;

    @TableField("fixed_delay")
    private Long fixedDelay;  // 固定延迟（毫秒）

    @TableField("fixed_rate")
    private Long fixedRate;   // 固定频率（毫秒）

    @TableField("initial_delay")
    private Long initialDelay; // 初始延迟（毫秒）
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
