package com.uniops.core.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * UniEntityRequestCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HttpLogRequestCondition extends PageCondition {
    Date startTime;
    Date endTime;
    String apiPath;
    String httpMethod;
    String statusCode;
    String ip;
}
