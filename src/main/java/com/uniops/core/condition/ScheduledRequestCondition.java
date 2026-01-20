package com.uniops.core.condition;

import com.uniops.core.entity.ScheduledConfig;
import lombok.Data;

/**
 * JobRequestCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/17
 */
@Data
public class ScheduledRequestCondition {
    private Long id;
    private Integer page;
    private Integer size;
    private Boolean enabled;
    private String name;
    private String monitorStatus;
    private ScheduledConfig configEntity;
}
