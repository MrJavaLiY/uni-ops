package com.uniops.starter.autoconfigure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@MapperScan("com.uniops.core.mapper")
@ComponentScan(basePackages = {"com.uniops.core"})
@EnableConfigurationProperties(UniOpsProperties.class)
@ConditionalOnProperty(prefix = "uniops", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UniOpsAutoConfiguration {

    // 分页插件配置已移至properties文件中
    // MyBatis-Plus分页插件现在通过配置文件控制：
    // mybatis-plus.configuration.map-underscore-to-camel-case=true
    // mybatis-plus.global-config.db-config.database-type=sql_server

}
