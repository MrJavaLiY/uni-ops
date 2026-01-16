package com.uniops.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.ScheduledLog;
import com.uniops.core.service.IScheduledConfigService;
import com.uniops.core.service.IScheduledLogService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/api/scheduled")
@Tag(name = "Scheduled任务监控", description = "@Scheduled注解任务监控API")
public class ScheduledMonitorController {

    @Autowired
    private IScheduledConfigService configService;

    @Autowired
    private IScheduledLogService logService;

    @Autowired
    private ApplicationContext applicationContext;

    @Operation(summary = "获取所有Scheduled方法")
    @GetMapping
    public ResponseEntity<List<ScheduledConfig>> getMethods() {
        return ResponseEntity.ok(configService.list());
    }

    @Operation(summary = "启用/禁用监控")
    @PutMapping("/{id}")
    public ResponseEntity<String> toggleMonitor(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {

        ScheduledConfig config = configService.getById(id);
        if (config != null) {
            config.setStatus(status);
            configService.updateById(config);
            return ResponseEntity.ok("状态已更新");
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "查询执行日志")
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @Parameter(description = "Bean名称") @RequestParam(required = false) String beanName,
            @Parameter(description = "方法名称") @RequestParam(required = false) String methodName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        var logs = logService.getLogs(beanName, methodName, page, size);
        return ResponseEntity.ok(Map.of(
                "content", logs.getRecords(),
                "totalElements", logs.getTotal(),
                "totalPages", logs.getPages()
        ));
    }

    @Operation(summary = "最近失败任务")
    @GetMapping("/recent-failures")
    public ResponseEntity<List<ScheduledLog>> getRecentFailures(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(logService.getRecentFailures(limit));
    }

    @Operation(summary = "手动触发方法")
    @PostMapping("/trigger/{beanName}/{methodName}")
    public ResponseEntity<String> triggerMethod(
            @Parameter(description = "Bean名称") @PathVariable String beanName,
            @Parameter(description = "方法名称") @PathVariable String methodName) {

        try {
            // 通过反射调用方法
            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);

            return ResponseEntity.ok("方法已触发");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
