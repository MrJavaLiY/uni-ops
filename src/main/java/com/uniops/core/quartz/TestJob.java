package com.uniops.core.quartz;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TestJob 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Component
public class TestJob {
    @Scheduled(cron = "0/5 * * * * ?")
    public void execute() {
        System.out.println("TestJob execute...");
    }
}
