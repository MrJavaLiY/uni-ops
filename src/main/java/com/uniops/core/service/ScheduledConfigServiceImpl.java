package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.condition.ScheduledRequestCondition;
import com.uniops.core.condition.SystemCondition;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.TaskMetadata;
import com.uniops.core.mapper.ScheduledConfigMapper;
import com.uniops.core.quartz.DynamicScheduledTaskManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ScheduledConfigServiceImpl 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Service
@Slf4j
public class ScheduledConfigServiceImpl extends ServiceImpl<ScheduledConfigMapper, ScheduledConfig>
        implements IScheduledConfigService {
    @Resource
    SystemCondition systemCondition;
    @Resource
    DynamicScheduledTaskManager dynamicScheduledTaskManager;

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
        config.setEnabled(enabled);
        config.setBeanName(beanName);
        config.setMethodName(methodName);
        config.setAppName(systemCondition.getApplicationName());
        this.updateById(config);
    }

    @Override
    public void updateData(ScheduledConfig config) {
        this.updateById(config);
    }


    @Override
    public Page<ScheduledConfig> pageByCondition(Page<ScheduledConfig> page, Long id, String name, Boolean enabled) {
        LambdaQueryWrapper<ScheduledConfig> wrapper = new LambdaQueryWrapper<>();

        if (id != null) {
            wrapper.eq(ScheduledConfig::getId, id);
        }
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(ScheduledConfig::getBeanName, name).or()
                    .like(ScheduledConfig::getMethodName, name);
        }
        if (enabled != null) {
            wrapper.eq(ScheduledConfig::getEnabled, enabled);
        }
//        wrapper.orderByDesc(ScheduledConfig::getId);
        page.addOrder(OrderItem.asc("id"));

        return this.page(page, wrapper);
    }


    @Override
    public void scanTask() {
        List<TaskMetadata> configs = dynamicScheduledTaskManager.scanAndOverrideScheduledTasks();
        for (TaskMetadata taskMetadata : configs) {
            ScheduledConfig config = taskMetadata.getConfig();
            // 5. 从数据库获取配置
            ScheduledConfig configD = getByBeanAndMethod(config.getBeanName(), config.getMethodName());
            if (configD != null) {
                // 6.根据配置决定如何处理
                if (configD.getEnabled() != null && configD.getEnabled()) {
                    // 启用：使用数据库的配置覆盖原有调度
                    dynamicScheduledTaskManager.overrideScheduledTask(config.getBeanName(), taskMetadata.getMethod(), configD, taskMetadata.getKey());
                } else {
                    // 禁用：停止该任务
                    log.warn("任务[{}]被配置为禁用，跳过调度", taskMetadata.getKey());
                    dynamicScheduledTaskManager.stopDynamicTask(taskMetadata.getKey());
                }
            } else {
                //初始化到数据库
                save(config);
                log.info("任务[{}]初始化完成", taskMetadata.getKey());
                dynamicScheduledTaskManager.overrideScheduledTask(config.getBeanName(), taskMetadata.getMethod(), config, taskMetadata.getKey());
            }
        }
    }

    @Override
    public void restartTask(ScheduledConfig config) {
        if (config.getEnabled() != null && !config.getEnabled()) {
            //关闭
            disableTask(config.getId());
            return;
        }
        //重新调度
        if (!StringUtils.isEmpty(config.getCronExpression()) && config.getFixedDelay() != -1 && config.getFixedRate() != -1) {
            throw new RuntimeException("任务执行周期配置重复，cron,fixedDelay,fixedRate三者只能配置一个");
        }
        if (!StringUtils.isEmpty(config.getCronExpression())) {
            dynamicScheduledTaskManager.updateTaskCron(config);
        } else {
            dynamicScheduledTaskManager.restartTask(config);
        }


    }


    @Override
    public void enableTask(Long id) {
        ScheduledConfig newConfig = getDataById(id);
        updateEnabled(newConfig.getBeanName(), newConfig.getMethodName(), true);
        dynamicScheduledTaskManager.enableTask(newConfig);
    }

    @Override
    public void disableTask(Long id) {
        ScheduledConfig config = getDataById(id);
        // 更新数据库状态
        updateEnabled(config.getBeanName(), config.getMethodName(), false);
        dynamicScheduledTaskManager.disableTask(config);
    }

    @Override
    public void restoreOriginal(Long id) {
        ScheduledConfig config = getDataById(id);
        updateEnabled(config.getBeanName(), config.getMethodName(), false);
    }

    @Override
    public ScheduledConfig getDataById(Long id) {
        ScheduledConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("任务不存在");
        }
        return config;
    }

    @Override
    public boolean updateConfigMes(ScheduledRequestCondition condition) {
        updateData(condition.getConfigEntity());
        ScheduledConfig config = condition.getConfigEntity();
        restartTask(config);
        return true;
    }



}
