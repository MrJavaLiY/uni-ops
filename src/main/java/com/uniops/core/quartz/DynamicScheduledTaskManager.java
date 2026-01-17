package com.uniops.core.quartz;

import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.service.IScheduledConfigService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DynamicScheduledTaskManager 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Component
@Slf4j
public class DynamicScheduledTaskManager implements ApplicationContextAware, InitializingBean, SmartLifecycle {

    @Autowired
    private IScheduledConfigService scheduledConfigService;

    @Autowired
    private TaskScheduler taskScheduler;  // 从配置类注入

//    @Resource
//    ScheduledTaskManager scheduledTaskManager;

    private ApplicationContext applicationContext;

    // 存储动态调度的Future
    private final Map<String, ScheduledFuture<?>> dynamicFutures = new ConcurrentHashMap<>();

    // 存储原始的Scheduled注解信息（用于恢复）
    private final Map<String, Scheduled> originalScheduledAnnotations = new ConcurrentHashMap<>();

    // 存储任务元数据
    private final Map<String, TaskMetadata> taskMetadataMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 在Bean初始化完成后执行
    }

    /**
     * 核心方法：扫描所有带@Scheduled的方法，根据配置覆盖调度
     */
    public void scanAndOverrideScheduledTasks() {
        log.info("开始扫描并覆盖@Scheduled任务...");

        //1. 获取所有Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);
            // 2. 获取所有方法
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                // 3. 检查是否有@Scheduled注解
                if (method.isAnnotationPresent(Scheduled.class)) {
                    Scheduled scheduled = method.getAnnotation(Scheduled.class);
                    // 4. 保存原始注解信息（用于恢复）
                    String key = generateKey(beanName, method.getName());
                    originalScheduledAnnotations.put(key, scheduled);
                    //有注解，就意味着肯定在执行，所以先一步列，以供后续的操作
//                    dynamicFutures.put(key,)

                    // 5. 从数据库获取配置
                    ScheduledConfig config = scheduledConfigService.getByBeanAndMethod(beanName, method.getName());

                    if (config != null) {
                        // 6.根据配置决定如何处理
                        if (config.getEnable() != null && config.getEnable()) {
                            // 启用：使用数据库的Cron覆盖原有调度
                            overrideScheduledTask(bean, method, config, key);
                        } else {
                            // 禁用：停止该任务
                            log.warn("任务[{}]被配置为禁用，跳过调度", key);
                            stopDynamicTask(key);
                        }

                        // 保存元数据
                        taskMetadataMap.put(key, new TaskMetadata(bean, method, config));
                    } else {
                        // 配置不存在，使用原始注解
                        log.info("任务[{}]未找到配置，使用原始@Scheduled注解", key);
                    }
                }
            }
        }

        log.info("扫描完成，共处理{}个任务", taskMetadataMap.size());
    }

    /**
     * 覆盖单个任务的调度
     */
    private void overrideScheduledTask(Object bean, Method method, ScheduledConfig config, String key) {
        try {
            // 先停止已存在的动态任务
            stopDynamicTask(key);

            // 创建可调用的任务
            Runnable task = () -> {
                try {
                    // 反射调用原方法
                    method.setAccessible(true);
                    method.invoke(bean);
                } catch (Exception e) {
                    log.error("执行任务[{}]失败", key, e);
                }
            };

            // 创建Cron触发器
            CronTrigger trigger = new CronTrigger(config.getCronExpression());

            // 调度任务
            ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
            dynamicFutures.put(key, future);

            log.info("成功覆盖任务[{}], Cron: {}", key, config.getCronExpression());

        } catch (Exception e) {
            log.error("覆盖任务[{}]失败", key, e);
        }
    }

    /**
     * 停止动态调度的任务
     */
    public void stopDynamicTask(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);
        stopDynamicTask(key);
    }

    private void stopDynamicTask(String key) {
        ScheduledFuture<?> future = dynamicFutures.get(key);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            dynamicFutures.remove(key);
            log.info("停止动态任务[{}]", key);
        }
    }

    /**
     * 启用任务（从配置表读取）
     */
    public void enableTask(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);
        ScheduledConfig config = scheduledConfigService.getByBeanAndMethod(beanName, methodName);

        if (config == null) {
            throw new RuntimeException("任务配置不存在: " + key);
        }

        // 更新数据库状态
        scheduledConfigService.updateEnabled(beanName, methodName, true);

        // 重新调度
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), config, key);
        }

        log.info("启用任务[{}]", key);
    }

    /**
     * 禁用任务
     */
    public void disableTask(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);

        // 更新数据库状态
        scheduledConfigService.updateEnabled(methodName, beanName, false);

        // 停止动态调度
        stopDynamicTask(key);

        log.info("禁用任务[{}]", key);
    }

    /**
     * 更新Cron表达式并重新调度
     */
    public void updateTaskCron(String beanName, String methodName, String newCron) {
        String key = generateKey(beanName, methodName);

        // 验证Cron表达式
        try {
           if (!CronExpression.isValidExpression(newCron)){
               throw new IllegalArgumentException("无效的Cron表达式: " + newCron);
           }
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的Cron表达式: " + newCron);
        }

        // 更新数据库
        ScheduledConfig newConfig = new ScheduledConfig();
        newConfig.setBeanName(beanName);
        newConfig.setMethodName(methodName);
        newConfig.setCronExpression(newCron);
        newConfig.setEnable(true);
        scheduledConfigService.updateData(newConfig);
        // 重新调度
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            ScheduledConfig config = scheduledConfigService.getByBeanAndMethod(beanName, methodName);
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), config, key);
        }
        log.info("更新任务[{}]的Cron为: {}", key, newCron);
    }

    /**
     * 恢复原始调度（使用@Scheduled注解）
     */
    public void restoreOriginal(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);

        // 停止动态任务
        stopDynamicTask(key);

        // 从数据库删除配置（或标记为禁用）
        scheduledConfigService.updateEnabled(methodName, beanName, false);

        log.info("恢复任务[{}]为原始@Scheduled配置", key);
    }

    /**
     * 获取所有任务状态
     */
    public Map<String, Object> getAllTaskStatus() {
        Map<String, Object> status = new HashMap<>();

        taskMetadataMap.forEach((key, metadata) -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("beanName", metadata.getBean().getClass().getSimpleName());
            taskInfo.put("methodName", metadata.getMethod().getName());
            // 检查动态任务状态
            ScheduledFuture<?> future = dynamicFutures.get(key);
            taskInfo.put("dynamicRunning", future != null && !future.isDone());

            // 检查配置
            ScheduledConfig config = metadata.getConfig();
            if (config != null) {
                taskInfo.put("enabled", config.getEnable());
                taskInfo.put("cron", config.getCronExpression());
            } else {
                taskInfo.put("enabled", true);
                taskInfo.put("cron", "使用原始@Scheduled");
            }

            status.put(key, taskInfo);
        });

        return status;
    }

    private String generateKey(String beanName, String methodName) {
        return beanName + "." + methodName;
    }
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scanAndOverrideScheduledTasks();
        }
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        // 设置较高的phase值，确保在其他组件之后启动
        return Integer.MAX_VALUE;
    }
    /**
     * 任务元数据
     */
    @Data
    @AllArgsConstructor
    private static class TaskMetadata {
        private Object bean;
        private Method method;
        private ScheduledConfig config;
    }
}

