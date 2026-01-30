package com.uniops.core.entity;

import lombok.Data;

/**
 * StatisticsHour 类的简要描述
 *
 * @author liyang
 * @since 2026/1/30
 */
@Data
public class StatisticsHour {
    private String hour;
    private long count;
    private long successCount;
    private long failedCount;
    private long total;
}
