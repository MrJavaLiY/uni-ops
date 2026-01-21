package com.uniops.core.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SystemRequestCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/21
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SystemRequestCondition extends PageCondition {
    private String secretKey;
    private String authorizationMes;
    private String systemId;
    private String systemName;
    private String ip;
    private Integer port;
    private Long id;
}
