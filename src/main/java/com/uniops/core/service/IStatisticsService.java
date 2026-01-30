// src/main/java/com/uniops/core/service/IStatisticsService.java
package com.uniops.core.service;

import com.uniops.core.entity.StatisticsHour;
import com.uniops.core.entity.StatisticsMes;
import com.uniops.core.vo.StatisticsVO;

import java.util.List;
import java.util.Map;

public interface IStatisticsService {

    /**
     * 获取今日每小时HTTP请求数统计
     */
    List<StatisticsHour> getTodayHourlyHttpRequestStats();

    /**
     * 获取今日每小时任务调度执行统计
     */
    List<StatisticsHour> getTodayHourlyScheduledStats();

    /**
     * 获取任务调度总执行数和成功数
     */
    Map<String, Long> getScheduledExecutionSummary();

    /**
     * 获取所有带CacheableEntity注解的表的数据量
     */
    List<StatisticsMes> getCacheableEntityCounts();

    /**
     * 获取今日每小时第三方HTTP调用次数统计
     */
    List<StatisticsHour> getTodayHourlyThirdPartyCallStats();

    /**
     * 获取完整的统计信息
     */
    StatisticsVO getFullStatistics();
}
