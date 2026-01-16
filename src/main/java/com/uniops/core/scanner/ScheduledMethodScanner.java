package com.uniops.core.scanner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.service.IScheduledConfigService;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Scheduled方法扫描器 自动扫描所有带@Scheduled注解的方法，注册到数据库
 */
@Slf4j
@Component
public class ScheduledMethodScanner implements SmartLifecycle {

    @Autowired
    @Lazy
    private ApplicationContext applicationContext;

    @Autowired
    @Lazy
    private IScheduledConfigService configService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public void scanScheduledMethods() {
        log.info("[UniOps] 开始扫描@Scheduled方法");
        // 获取所有已加载的Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map<String, Object> allBeans = new java.util.HashMap<>();

        // 获取所有Bean实例
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                // 排除自身实例
                if (bean != this) {
                    allBeans.put(beanName, bean);
                }
            } catch (Exception e) {
                log.debug("[UniOps] 获取Bean '{}' 实例失败: {}", beanName, e.getMessage());
            }
        }

        int count = 0;

        for (Map.Entry<String, Object> entry : allBeans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

            // 扫描Bean的所有方法
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Scheduled.class)) {
                    Scheduled scheduled = method.getAnnotation(Scheduled.class);
                    String methodName = method.getName();
                    String cronExpression = parseCron(scheduled);
                    // 检查是否已存在
                    ScheduledConfig existing = configService.getByBeanAndMethod(beanName, methodName);
                    if (existing != null) {
                        log.info("[UniOps] 方法已存在: {}.{} (跳过)", beanName, methodName);
                        continue;
                    }
                    // 注册到数据库
                    ScheduledConfig config = new ScheduledConfig();
                    config.setBeanName(beanName);
                    config.setMethodName(methodName);
                    config.setCronExpression(cronExpression);
                    config.setStatus("DISABLED");  // 默认禁用，避免意外执行
                    config.setDescription("自动发现的@Scheduled方法");
                    config.setCreatedAt(new Date());
                    config.setUpdatedAt(new Date());

                    configService.save(config);
                    count++;
                    log.info("[UniOps] 已注册@Scheduled方法: {}.{}", beanName, methodName);
                }
            }
        }

        log.info("[UniOps] @Scheduled方法扫描完成，共注册 {} 个方法", count);
    }

    /**
     * 解析Cron表达式
     */
    private String parseCron(Scheduled scheduled) {
        if (!scheduled.cron().isEmpty()) {
            return scheduled.cron();
        }

        long fixedRate = scheduled.fixedRate();
        if (fixedRate != -1) {
            return "0/" + (fixedRate / 1000) + " * * * * ?";
        }

        long fixedDelay = scheduled.fixedDelay();
        if (fixedDelay != -1) {
            return "0/" + (fixedDelay / 1000) + " * * * * ?";
        }

        long initialDelay = scheduled.initialDelay();
        if (initialDelay != -1) {
            return "0/" + (initialDelay / 1000) + " * * * * ?";
        }

        return "0 0 0 * * ?";  // 默认每天凌晨
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
            scanScheduledMethods();
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
}
