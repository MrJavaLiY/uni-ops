package com.uniops.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uniops.core.entity.ScheduledConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduledConfigMapper extends BaseMapper<ScheduledConfig> {
    @PostConstruct
    default void init() {
        System.out.println("初始化");
    }
}
