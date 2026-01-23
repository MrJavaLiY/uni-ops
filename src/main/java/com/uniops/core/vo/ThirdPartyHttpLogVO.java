package com.uniops.core.vo;

import com.uniops.core.entity.HttpRequestLog;
import com.uniops.core.entity.ThirdPartyHttpLog;
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
public class ThirdPartyHttpLogVO extends ViewListEntity<ThirdPartyHttpLog> {
}
