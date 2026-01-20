package com.uniops.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * 任务元数据
 */
@Data
@AllArgsConstructor
public  class TaskMetadata {
    private Object bean;
    private Method method;
    private ScheduledConfig config;
    private String key;
}
