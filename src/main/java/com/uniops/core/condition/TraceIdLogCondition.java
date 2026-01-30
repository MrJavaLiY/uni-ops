package com.uniops.core.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * TraceIdLogCondition 类的简要描述
 * 用于根据traceId和时间范围检索日志
 *
 * @author liyang
 * @since 2026/1/30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TraceIdLogCondition extends PageCondition {
    private String traceId;      // 链路ID
    private Date startTime;      // 开始时间
    private Date endTime;        // 结束时间
}
