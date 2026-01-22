package com.uniops.core.annotation;

import java.lang.annotation.*;

/**
 * 数据库实体类注解
 * 用于标记需要被缓存和管理的实体类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheableEntity {
    /**
     * 实体类名称，用于缓存标识
     */
    String value() default "";

    /**
     * 表名（可选）
     */
    String tableName() default "";

    /**
     * 主键字段名
     */
    String primaryKey() default "id";
}
