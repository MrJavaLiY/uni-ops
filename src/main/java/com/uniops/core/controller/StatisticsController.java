// src/main/java/com/uniops/core/controller/StatisticsController.java
package com.uniops.core.controller;

import com.uniops.core.entity.StatisticsHour;
import com.uniops.core.entity.StatisticsMes;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.IStatisticsService;
import com.uniops.core.vo.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@Tag(name = "系统统计", description = "系统各项指标统计API")
public class StatisticsController {

    @Autowired
    private IStatisticsService statisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘统计信息", description = "获取所有四个图表所需的数据")
    public ResponseResult<StatisticsVO> getDashboardStats() {
        return ResponseResult.success(statisticsService.getFullStatistics());
    }

    @GetMapping("/http-requests-today")
    @Operation(summary = "获取今日每小时HTTP请求统计", description = "图表1：统计每日的HTTP请求数，按小时统计当天数据")
    public ResponseResult<List<StatisticsHour>> getTodayHourlyHttpRequestStats() {
        return ResponseResult.success(statisticsService.getTodayHourlyHttpRequestStats());
    }

    @GetMapping("/scheduled-stats-today")
    @Operation(summary = "获取今日每小时任务调度统计", description = "图表2：统计任务调度执行情况，按小时统计当前的总执行数和成功数")
    public ResponseResult<List<StatisticsHour>> getTodayHourlyScheduledStats() {
        return ResponseResult.success(statisticsService.getTodayHourlyScheduledStats());
    }

    @GetMapping("/scheduled-summary")
    @Operation(summary = "获取任务调度汇总统计", description = "获取任务调度的总执行数和成功数")
    public ResponseResult<Map<String, Long>> getScheduledExecutionSummary() {
        return ResponseResult.success(statisticsService.getScheduledExecutionSummary());
    }

    @GetMapping("/cacheable-entity-counts")
    @Operation(summary = "获取CacheableEntity表数据量", description = "图表3：统计带CacheableEntity注解的表的现存数据量")
    public ResponseResult<List<StatisticsMes>> getCacheableEntityCounts() {
        return ResponseResult.success(statisticsService.getCacheableEntityCounts());
    }

    @GetMapping("/third-party-calls-today")
    @Operation(summary = "获取今日每小时第三方HTTP调用统计", description = "图表4：按小时统计第三方HTTP调用次数")
    public ResponseResult<List<StatisticsHour>> getTodayHourlyThirdPartyCallStats() {
        return ResponseResult.success(statisticsService.getTodayHourlyThirdPartyCallStats());
    }
}
