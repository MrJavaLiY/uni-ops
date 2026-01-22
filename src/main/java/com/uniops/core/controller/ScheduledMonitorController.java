package com.uniops.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniops.core.condition.ScheduledRequestCondition;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.vo.SchedulerLogVO;
import com.uniops.core.vo.SchedulerVO;
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
@RequestMapping("/scheduled-jobs")
@Tag(name = "Scheduled任务监控", description = "@Scheduled注解任务监控API")
public class ScheduledMonitorController {

    @Autowired
    private IScheduledConfigService configService;

    @Autowired
    private IScheduledLogService logService;


    @Autowired
    private ApplicationContext applicationContext;


    @Operation(summary = "修改配置")
    @PostMapping("update")
    public ResponseResult<Boolean> updateConfig(@RequestBody ScheduledRequestCondition condition) {
        if (condition.getConfigEntity().getId() == null || condition.getConfigEntity().getId() == 0) {
            return ResponseResult.error("请选择要修改的配置");
        }
        if (condition.getConfigEntity().getInitialDelay() == null) {
            condition.getConfigEntity().setInitialDelay((long) -1);
        }
        if (condition.getConfigEntity().getFixedRate() == null) {
            condition.getConfigEntity().setFixedRate((long) -1);
        }
        if (condition.getConfigEntity().getFixedDelay() == null) {
            condition.getConfigEntity().setFixedDelay((long) -1);
        }

        boolean success = configService.updateConfigMes(condition);
        return ResponseResult.success(success);
    }

    @Operation(summary = "获取所有Scheduled方法")
    @PostMapping("search")
    public ResponseResult<SchedulerVO> getMethods(@RequestBody ScheduledRequestCondition condition) {
        // 设置默认分页参数
        Integer page = condition.getPage() != null ? condition.getPage() : 1;
        Integer size = condition.getSize() != null ? condition.getSize() : 10;

        // 创建分页对象
        Page<ScheduledConfig> pageParam = new Page<>(page, size);

        // 执行分页查询
        Page<ScheduledConfig> resultPage = configService.pageByCondition(pageParam, condition.getId(), condition.getName(), condition.getEnabled());

        // 构建返回结果
        SchedulerVO vo = new SchedulerVO();
        vo.setPage((int) resultPage.getCurrent());
        vo.setSize((int) resultPage.getSize());
        vo.setTotal(resultPage.getTotal());
        vo.setRecords(resultPage.getRecords());
        return ResponseResult.success(vo);
    }

    @Operation(summary = "启用/禁用监控")
    @PutMapping("/{id}")
    public ResponseEntity<String> toggleMonitor(@RequestBody ScheduledRequestCondition condition) {

        ScheduledConfig config = configService.getDataById(condition.getId());
        if (config != null) {
            config.setMonitorStatus(condition.getMonitorStatus());
            configService.updateById(config);
            return ResponseEntity.ok("状态已更新");
        }
        return ResponseEntity.notFound().build();
    }


    @Operation(summary = "启用任务")
    @PutMapping("/enable/{beanName}/{methodName}")
    public ResponseEntity<String> enableTask(@RequestBody ScheduledRequestCondition condition) {
        try {
            configService.enableTask(condition.getId());
            return ResponseEntity.ok("任务已启用");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("启用任务失败: " + e.getMessage());
        }
    }

    @Operation(summary = "禁用任务")
    @PutMapping("/disable/{beanName}/{methodName}")
    public ResponseEntity<String> disableTask(@RequestBody ScheduledRequestCondition condition) {
        try {
            configService.disableTask(condition.getId());
            return ResponseEntity.ok("任务已禁用");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("禁用任务失败: " + e.getMessage());
        }
    }


    @Operation(summary = "停止任务")
    @DeleteMapping("/{beanName}/{methodName}")
    public ResponseEntity<String> stopTask(@RequestBody ScheduledRequestCondition condition) {
        try {
            configService.disableTask(condition.getId());
            return ResponseEntity.ok("任务已停止");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("停止任务失败: " + e.getMessage());
        }
    }

    @Operation(summary = "恢复原始任务配置")
    @PutMapping("/restore/{beanName}/{methodName}")
    public ResponseEntity<String> restoreOriginal(@RequestBody ScheduledRequestCondition condition) {
        try {
            configService.restoreOriginal(condition.getId());
            return ResponseEntity.ok("任务已恢复到原始配置");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("恢复原始配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询执行日志")
    @PostMapping("/logs")
    public ResponseResult<SchedulerLogVO> getLogs(@RequestBody ScheduledRequestCondition condition) {
        var logs = logService.getLogs(condition);
        SchedulerLogVO vo = new SchedulerLogVO();
        vo.setPage((int) logs.getCurrent());
        vo.setSize((int) logs.getSize());
        vo.setTotal(logs.getTotal());
        vo.setRecords(logs.getRecords());
        return ResponseResult.success(vo);
    }

    @Operation(summary = "最近失败任务")
    @GetMapping("/recent-failures")
    public ResponseEntity<List<ScheduledLog>> getRecentFailures(@Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(logService.getRecentFailures(limit));
    }

    @Operation(summary = "手动触发方法")
    @PostMapping("/trigger/{beanName}/{methodName}")
    public ResponseEntity<String> triggerMethod(@Parameter(description = "Bean名称") @PathVariable String beanName, @Parameter(description = "方法名称") @PathVariable String methodName) {

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
