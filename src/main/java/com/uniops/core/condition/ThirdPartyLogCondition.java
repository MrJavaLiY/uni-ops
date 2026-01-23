package com.uniops.core.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * ThirdPartyLogCondtion 类的简要描述
 *
 * @author liyang
 * @since 2026/1/23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ThirdPartyLogCondition extends PageCondition {
    String thirdPartyName;
    String url;
    Date startTime;
    Date endTime;
}
