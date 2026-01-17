package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.entity.ScheduledConfig;

/**
 * IScheduledConfigService 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
public interface IScheduledConfigService extends IService<ScheduledConfig> {
    ScheduledConfig getByBeanAndMethod(String beanName, String methodName);
 void updateEnabled(String beanName,String methodName,boolean enabled);
    void updateData(ScheduledConfig config);
}
