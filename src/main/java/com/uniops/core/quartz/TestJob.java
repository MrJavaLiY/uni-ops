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
    @Scheduled(cron = "0/5 * * * * ?")
    public void execute1() {
        System.out.println("TestJob1 execute...");
    }
    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void execute2() {
        System.out.println("TestJob2 execute...");
    }
    @Scheduled(fixedRate = 5000, initialDelay = 1000)
    public void execute3() {
        System.out.println("TestJob3 execute...");
    }
    @Scheduled(fixedDelay = 5000)
    public void execute4() {
        System.out.println("TestJob4 execute...");
    }
}
