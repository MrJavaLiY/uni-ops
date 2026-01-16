package com.uniops.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI uniOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniOps API文档")
                        .description("统一运维平台 - 任务调度监控API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UniOps Team")
                                .email("contact@uniops.io")
                                .url("https://uniops.io"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0"))
                        .termsOfService("https://uniops.io/terms"));
    }
}
