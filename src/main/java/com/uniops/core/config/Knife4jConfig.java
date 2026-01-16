package com.uniops.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

/**
 * Knife4j配置（Swagger增强UI）
 */
@Configuration
@EnableKnife4j
public class Knife4jConfig {

    @Bean
    @Profile({"dev", "test"})
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniOps API文档")
                        .description("统一运维平台API接口文档")
                        .contact(new Contact().name("开发团队"))
                        .version("1.0"));
    }
}
