package com.uniops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.uniops"})
@EnableScheduling
@MapperScan("com.uniops.core.mapper")
public class UniOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniOpsApplication.class, args);
    }

}
