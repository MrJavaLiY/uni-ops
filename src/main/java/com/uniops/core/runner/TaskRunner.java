package com.uniops.core.runner;

import com.uniops.core.service.IScheduledConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * TaskRunner 类的简要描述
 *
 * @author liyang
 * @since 2026/1/20
 */
@Slf4j
@Component
public class TaskRunner implements ApplicationRunner {
    @Resource
    IScheduledConfigService scheduledConfigService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("任务调度启动");
        scheduledConfigService.scanTask();
        log.info("任务调度启动完成");
    }
}
