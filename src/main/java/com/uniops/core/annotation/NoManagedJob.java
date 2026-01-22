package com.uniops.core.annotation;// src/main/java/com/uniops/core/quartz/ManagedJob.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoManagedJob {
}
