package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * Scheduled任务配置实体
 * 用于存储@Scheduled注解方法的配置信息
 */
@Data
@TableName("uniops_scheduled_config")
public class ScheduledConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bean_name")
    private String beanName;  // Bean名称（如：businessService）

    @TableField("method_name")
    private String methodName;  // 方法名（如：syncData）

    @TableField("cron_expression")
    private String cronExpression;  // Cron表达式（如：0/30 * * * * ?）

    @TableField("status")
    private String status;  //状态：ENABLED/DISABLED

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
