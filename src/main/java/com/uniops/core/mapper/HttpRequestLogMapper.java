// src/main/java/com/uniops/core/mapper/HttpRequestLogMapper.java
package com.uniops.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uniops.core.entity.HttpRequestLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HttpRequestLogMapper extends BaseMapper<HttpRequestLog> {
}
