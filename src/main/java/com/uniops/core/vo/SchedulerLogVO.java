package com.uniops.core.vo;

import com.uniops.core.entity.ScheduledConfig;
import com.uniops.core.entity.ScheduledLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SchedulerLogVO 类的简要描述
 *
 * @author liyang
 * @since 2026/1/22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SchedulerLogVO extends ViewListEntity<ScheduledLog> {
}
