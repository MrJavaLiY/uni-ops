package com.uniops.core.vo;

import com.uniops.core.entity.LogInfo;
import com.uniops.core.entity.ScheduledLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * LogInfoVO 类的简要描述
 *
 * @author liyang
 * @since 2026/1/30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LogInfoVO extends ViewListEntity<LogInfo> {
    public LogInfoVO(int page, int size, long total, List<LogInfo> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
}
