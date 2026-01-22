package com.uniops.core.quartz;

import com.uniops.core.annotation.NoManagedJob;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.TaskMetadata;
import com.uniops.core.service.IScheduledConfigService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DynamicScheduledTaskManager implements ApplicationContextAware, InitializingBean {


    @Autowired
    private TaskScheduler taskScheduler;  // 从配置类注入


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
    public List<TaskMetadata> scanAndOverrideScheduledTasks() {
        log.info("开始扫描并覆盖@Scheduled任务...");
        List<TaskMetadata> scheduledConfigs = new ArrayList<>();
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
                    ScheduledConfig newConfig = new ScheduledConfig();
                    newConfig.setBeanName(beanName);
                    newConfig.setMethodName(method.getName());
                    newConfig.setCronExpression(scheduled.cron());
                    newConfig.setFixedDelay(scheduled.fixedDelay());
                    newConfig.setFixedRate(scheduled.fixedRate());
                    newConfig.setInitialDelay(scheduled.initialDelay());
                    newConfig.setAppName(applicationContext.getEnvironment().getProperty("spring.application.name"));
                    newConfig.setEnabled(true);
                    newConfig.setMonitorStatus("ENABLED");
                    newConfig.setDescription("自动生成的配置");
//                    overrideScheduledTask(bean, method, newConfig, key);
                    TaskMetadata taskMetadata = new TaskMetadata(bean, method, newConfig, key);
                    scheduledConfigs.add(taskMetadata);
                    // 保存元数据
                    taskMetadataMap.put(key, taskMetadata);
                }
            }
        }
        log.info("扫描完成，共处理{}个任务", taskMetadataMap.size());
        return scheduledConfigs;
    }

    /**
     * 覆盖单个任务的调度
     */
    public void overrideScheduledTask(Object bean, Method method, ScheduledConfig config, String key) {
        /**
         * 覆盖单个任务的调度
         */
        try {
            // 先停止已存在的动态任务
            stopDynamicTask(key);

            // 创建可调用的任务
            Runnable task = () -> {
                try {
                    // 获取真正的目标对象（处理AOP代理情况）
                    Object targetObject = AopProxyUtils.ultimateTargetClass(bean).cast(bean);
                    // 或者简单地从ApplicationContext重新获取bean实例
                    Object actualBean = applicationContext.getBean(method.getDeclaringClass());

                    // 反射调用原方法
                    method.setAccessible(true);
                    method.invoke(actualBean, new Object[0]); // 添加空参数列表
                } catch (Exception e) {
                    log.error("执行任务[{}]失败", key, e);
                }
            };

            // 根据配置类型选择不同的调度方式
            ScheduledFuture<?> future;
            if (config.getFixedDelay() != null && config.getFixedDelay() > 0) {
                // 使用固定延迟调度
                if (config.getInitialDelay() != null && config.getInitialDelay() > 0) {
                    // 使用固定延迟调度，带初始延迟
                    Instant startTime = Instant.now().plusMillis(config.getInitialDelay());
                    future = taskScheduler.scheduleWithFixedDelay(task,
                            startTime,
                            Duration.ofMillis(config.getFixedDelay()));
                } else {
                    future = taskScheduler.scheduleWithFixedDelay(task,
                            Duration.ofMillis(config.getFixedDelay()));
                }
            } else if (config.getFixedRate() != null && config.getFixedRate() > 0) {
                // 使用固定频率调度
                if (config.getInitialDelay() != null && config.getInitialDelay() > 0) {
                    // 使用固定频率调度，带初始延迟
                    Instant startTime = Instant.now().plusMillis(config.getInitialDelay());
                    future = taskScheduler.scheduleAtFixedRate(task,
                            startTime,
                            Duration.ofMillis(config.getFixedRate()));
                } else {
                    future = taskScheduler.scheduleAtFixedRate(task,
                            Duration.ofMillis(config.getFixedRate()));
                }
            } else {
                // 使用Cron表达式调度
                if (config.getCronExpression() != null && !config.getCronExpression().isEmpty()) {
                    CronTrigger trigger = new CronTrigger(config.getCronExpression());
                    future = taskScheduler.schedule(task, trigger);
                } else {
                    log.warn("任务[{}]没有有效的调度配置，跳过调度", key);
                    return;
                }
            }

            // 调度任务
            dynamicFutures.put(key, future);

            log.info("成功调度任务[{}], 类型: {}, 参数: {}", key,
                    config.getFixedDelay() != null ? "FIXED_DELAY" :
                            config.getFixedRate() != null ? "FIXED_RATE" : "CRON",
                    config.getFixedDelay() != null ? config.getFixedDelay() + "ms" :
                            config.getFixedRate() != null ? config.getFixedRate() + "ms" : config.getCronExpression());

        } catch (Exception e) {
            log.error("调度任务[{}]失败", key, e);
        }
    }

    /**
     * 更新任务的fixedDelay配置
     */
    public void updateTaskFixedDelay(ScheduledConfig config) {
        String key = generateKey(config.getBeanName(), config.getMethodName());


        // 重新调度
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), config, key);
        }
        log.info("更新任务[{}]的FixedDelay为: {}ms", key, config.getFixedDelay());
    }

    /**
     * 更新任务的fixedRate配置
     */
    public void updateTaskFixedRate(ScheduledConfig config) {
        String key = generateKey(config.getBeanName(), config.getMethodName());

        // 重新调度
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), config, key);
        }
        log.info("更新任务[{}]的FixedRate为: {}ms", key, config.getFixedRate());
    }

    /**
     * 更新任务的初始延迟
     */
    public void updateTaskInitialDelay(ScheduledConfig config) {
        String key = generateKey(config.getBeanName(), config.getMethodName());
        // 重新调度任务以应用初始延迟
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), config, key);
        }
    }

    /**
     * 停止动态调度的任务
     */
    public void stopDynamicTask(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);
        stopDynamicTask(key);
    }

    public void stopDynamicTask(String key) {
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
    public void enableTask(ScheduledConfig config) {
        String key = generateKey(config.getBeanName(), config.getMethodName());
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
    public void disableTask(ScheduledConfig newConfig) {
        String key = generateKey(newConfig.getBeanName(), newConfig.getMethodName());
        // 停止动态调度
        stopDynamicTask(key);

        log.info("禁用任务[{}]", key);
    }

    /**
     * 更新Cron表达式并重新调度
     */
    public void updateTaskCron(ScheduledConfig newConfig) {
        String key = generateKey(newConfig.getBeanName(), newConfig.getMethodName());

        // 验证Cron表达式
        try {
            if (!CronExpression.isValidExpression(newConfig.getCronExpression())) {
                throw new IllegalArgumentException("无效的Cron表达式: " + newConfig.getCronExpression());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的Cron表达式: " + newConfig.getCronExpression());
        }

        // 重新调度
        TaskMetadata metadata = taskMetadataMap.get(key);
        if (metadata != null) {
            overrideScheduledTask(metadata.getBean(), metadata.getMethod(), newConfig, key);
        }
        log.info("更新任务[{}]的Cron为: {}", key, newConfig.getCronExpression());
    }

    /**
     * 恢复原始调度（使用@Scheduled注解）
     */
    public void restoreOriginal(String beanName, String methodName) {
        String key = generateKey(beanName, methodName);

        // 停止动态任务
        stopDynamicTask(key);

        log.info("恢复任务[{}]为原始@Scheduled配置", key);
    }

    /**
     * 重启任务，主要是为了刷新配置
     *
     */
    public void restartTask(ScheduledConfig config) {
        disableTask(config);
        enableTask(config);
    }


    /**
     * 获取所有任务状态
     */
    public List<Map<String, Object>> getAllTaskStatus() {
        List<Map<String, Object>> status = new ArrayList<>();
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
                taskInfo.put("enabled", config.getEnabled());
                taskInfo.put("cron", config.getCronExpression());
                taskInfo.put("fixedDelay", config.getFixedDelay());
                taskInfo.put("fixedRate", config.getFixedRate());
                taskInfo.put("initialDelay", config.getInitialDelay());
            } else {
                taskInfo.put("status", true);
                taskInfo.put("cron", "使用原始@Scheduled");
            }

            status.add(taskInfo);
        });

        return status;
    }

    private String generateKey(String beanName, String methodName) {
        return beanName + "." + methodName;
    }

    public void stop() {
        // 停止所有正在运行的任务
        dynamicFutures.forEach((key, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
        });
        dynamicFutures.clear();
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }


}
