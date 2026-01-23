package com.uniops.core.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class UniEntityRequestCondition extends PageCondition{
    private String entityName;
    private Map<String,Object> conditions =new HashMap<>();
    private Map<String,Object> data = new HashMap<>();
}
