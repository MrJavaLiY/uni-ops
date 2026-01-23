package com.uniops.core.entity;

import lombok.Data;

/**
 * UniEntity 类的简要描述
 *
 * @author liyang
 * @since 2026/1/22
 */
@Data
public class UniEntity {
    private String columnName;
    private String columnType;
    private Boolean primaryKey;
    private String primaryType;
}
