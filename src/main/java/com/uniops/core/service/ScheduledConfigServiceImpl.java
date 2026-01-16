package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.mapper.ScheduledConfigMapper;
import org.springframework.stereotype.Service;

/**
 * ScheduledConfigServiceImpl 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Service
public class ScheduledConfigServiceImpl extends ServiceImpl<ScheduledConfigMapper, ScheduledConfig>
        implements IScheduledConfigService {

    @Override
    public ScheduledConfig getByBeanAndMethod(String beanName, String methodName) {
        return this.getOne(
                new LambdaQueryWrapper<ScheduledConfig>()
                        .eq(ScheduledConfig::getBeanName, beanName)
                        .eq(ScheduledConfig::getMethodName, methodName)
        );
    }
}
