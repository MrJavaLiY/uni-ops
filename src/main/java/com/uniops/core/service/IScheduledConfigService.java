package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.condition.ScheduledRequestCondition;
import com.uniops.core.entity.ScheduledConfig;

/**
 * IScheduledConfigService 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
public interface IScheduledConfigService extends IService<ScheduledConfig> {
    ScheduledConfig getByBeanAndMethod(String beanName, String methodName);

    void updateEnabled(String beanName, String methodName, boolean enabled);

    void updateData(ScheduledConfig config);

    /**
     * 分页条件查询定时任务配置
     *
     * @param page    分页参数
     * @param id      ID条件
     * @param name    名称条件
     * @param enabled 状态条件
     * @return 分页结果
     */
    Page<ScheduledConfig> pageByCondition(Page<ScheduledConfig> page, Long id, String name, Boolean enabled);

    void scanTask();

    void restartTask(ScheduledConfig config);

    void enableTask(Long id);

    /**
     * 禁用任务
     */
    public void disableTask(Long id);

    void restoreOriginal(Long id);

    ScheduledConfig getDataById(Long id);

    boolean updateConfigMes(ScheduledRequestCondition condition);
}
