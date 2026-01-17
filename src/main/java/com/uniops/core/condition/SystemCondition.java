package com.uniops.core.condition;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * SystemCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Data
@Configuration
public class SystemCondition {
    @Value("${spring.application.name}")
    private String applicationName;
}
