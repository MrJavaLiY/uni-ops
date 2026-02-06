package com.uniops.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标识需要权限校验的API接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuth {
    /**
     * API路径
     */
    String[] value() default {};

    /**
     * 是否需要认证
     */
    boolean required() default true;
}
