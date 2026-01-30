// src/main/java/com/uniops/core/vo/StatisticsVO.java
package com.uniops.core.vo;

import com.uniops.core.entity.StatisticsHour;
import com.uniops.core.entity.StatisticsMes;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 统计信息视图对象
 */
@Data
public class StatisticsVO {

    // 图表1: 每日HTTP请求数统计
    private List<StatisticsHour> hourlyHttpRequestStats;

    // 图表2: 任务调度执行情况统计
    private List<StatisticsHour> hourlyScheduledStats;
    private long totalScheduledExecutions;
    private long successfulScheduledExecutions;

    // 图表3: CacheableEntity表的数据量统计
    private List<StatisticsMes> cacheableEntityCounts;

    // 图表4: 第三方HTTP调用次数统计
    private List<StatisticsHour> hourlyThirdPartyCallStats;
}
