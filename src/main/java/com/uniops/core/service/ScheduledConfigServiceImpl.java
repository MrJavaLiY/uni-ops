package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.condition.SystemCondition;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.mapper.ScheduledConfigMapper;
import jakarta.annotation.Resource;
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
@Resource
    SystemCondition systemCondition;
    @Override
    public ScheduledConfig getByBeanAndMethod(String beanName, String methodName) {
        return this.getOne(
                new LambdaQueryWrapper<ScheduledConfig>()
                        .eq(ScheduledConfig::getBeanName, beanName)
                        .eq(ScheduledConfig::getMethodName, methodName)

        );
    }

    @Override
    public void updateEnabled(String beanName, String methodName, boolean enabled) {
        ScheduledConfig config = new ScheduledConfig();
        config.setEnable(enabled);
        config.setBeanName(beanName);
        config.setMethodName(methodName);
        config.setAppName(systemCondition.getApplicationName());
        this.updateById(config);
    }

    @Override
    public void updateData(ScheduledConfig config) {
        this.updateById(config);
    }


}
