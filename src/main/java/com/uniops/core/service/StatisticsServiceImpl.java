// src/main/java/com/uniops/core/service/impl/StatisticsServiceImpl.java
package com.uniops.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.uniops.core.annotation.CacheableEntity;
import com.uniops.core.cache.EntityCacheManager;
import com.uniops.core.entity.*;
import com.uniops.core.mapper.*;
import com.uniops.core.service.IStatisticsService;
import com.uniops.core.service.ISystemRegisterService;
import com.uniops.core.service.ISystemManagerService;
import com.uniops.core.vo.StatisticsVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Resource
    private HttpRequestLogMapper httpRequestLogMapper;

    @Resource
    private ScheduledLogMapper scheduledLogMapper;

    @Resource
    private ThirdPartyHttpLogMapper thirdPartyHttpLogMapper;

    @Resource
    private ScheduledConfigMapper scheduledConfigMapper;

    @Resource
    private SystemRegisterMapper systemRegisterMapper;

    @Autowired
    private EntityCacheManager entityCacheManager;

    @Autowired
    private ISystemRegisterService systemRegisterService;

    @Override
    public List<StatisticsHour> getTodayHourlyHttpRequestStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        QueryWrapper<HttpRequestLog> wrapper = new QueryWrapper<>();
        wrapper.between("request_time", startOfDay, endOfDay);
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        wrapper.select("FORMAT(request_time, '%H') as hour, count(*) as count")
                .groupBy("FORMAT(request_time, '%H')")
                .orderByAsc("hour");

        List<Map<String, Object>> result = httpRequestLogMapper.selectMaps(wrapper);

        // 初始化每小时数据为0
        Map<Integer, Long> hourCountMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCountMap.put(i, 0L);
        }

        // 填充实际数据
        for (Map<String, Object> item : result) {
            String hourStr = String.valueOf(item.get("hour"));
            Integer hour = Integer.parseInt(hourStr);
            Long count = Long.parseLong(String.valueOf(item.get("count")));
            hourCountMap.put(hour, count);
        }

        // 构建返回数据
        List<StatisticsHour> stats = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            StatisticsHour stat = new StatisticsHour();
            stat.setHour(String.format("%02d:00", i));
            stat.setCount(hourCountMap.get(i));
            stats.add(stat);
        }

        return stats;
    }

    @Override
    public List<StatisticsHour> getTodayHourlyScheduledStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        QueryWrapper<ScheduledLog> wrapper = new QueryWrapper<>();
        wrapper.between("trigger_time", startOfDay, endOfDay);
        wrapper.eq("app_name", systemRegisterService.localSystem().getSystemId())
                .select("FORMAT(trigger_time, '%H') as hour, status, count(*) as count")
                .groupBy("FORMAT(trigger_time, '%H'), status")
                .orderByAsc("hour");

        List<Map<String, Object>> result = scheduledLogMapper.selectMaps(wrapper);

        // 初始化每小时数据为0
        Map<Integer, Map<String, Long>> hourStatusMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Long> statusCount = new HashMap<>();
            statusCount.put("SUCCESS", 0L);
            statusCount.put("FAILED", 0L);
            hourStatusMap.put(i, statusCount);
        }

        // 填充实际数据
        for (Map<String, Object> item : result) {
            String hourStr = String.valueOf(item.get("hour"));
            Integer hour = Integer.parseInt(hourStr);
            String status = String.valueOf(item.get("status"));
            Long count = Long.parseLong(String.valueOf(item.get("count")));

            hourStatusMap.get(hour).put(status, count);
        }

        // 构建返回数据
        List<StatisticsHour> stats = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            StatisticsHour stat = new StatisticsHour();
            stat.setHour(String.format("%02d:00", i));
            stat.setSuccessCount(hourStatusMap.get(i).get("SUCCESS"));
            stat.setFailedCount(hourStatusMap.get(i).get("FAILED"));
            stat.setTotal(hourStatusMap.get(i).get("SUCCESS") + hourStatusMap.get(i).get("FAILED"));
            stats.add(stat);
        }

        return stats;
    }

    @Override
    public Map<String, Long> getScheduledExecutionSummary() {
        QueryWrapper<ScheduledLog> totalWrapper = new QueryWrapper<>();
        totalWrapper.eq("app_name", systemRegisterService.localSystem().getSystemId());
        long total = scheduledLogMapper.selectCount(totalWrapper);

        QueryWrapper<ScheduledLog> successWrapper = new QueryWrapper<>();
        successWrapper.eq("app_name", systemRegisterService.localSystem().getSystemId());
        successWrapper.eq("status", "SUCCESS");
        long success = scheduledLogMapper.selectCount(successWrapper);

        Map<String, Long> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("success", success);

        return summary;
    }

    @Override
    public List<StatisticsMes> getCacheableEntityCounts() {
        List<StatisticsMes> counts = new ArrayList<>();

        // 手动列出所有已知的带CacheableEntity注解的实体类及其对应的Mapper
        Map<Class<?>, Object> entityMapperMap = getKnownEntityMappers();

        for (Map.Entry<Class<?>, Object> entry : entityMapperMap.entrySet()) {
            Class<?> entityClass = entry.getKey();
            Object mapper = entry.getValue();

            if (entityClass.isAnnotationPresent(CacheableEntity.class)) {
                CacheableEntity cacheableEntity = entityClass.getAnnotation(CacheableEntity.class);
                String entityName = cacheableEntity.value().isEmpty() ?
                    entityClass.getSimpleName() : cacheableEntity.value();

                String tableName = cacheableEntity.tableName().isEmpty() ?
                    getDefaultTableName(entityClass) : cacheableEntity.tableName();

                // 通过反射获取Mapper的selectCount方法
                long count = getEntityCountFromMapper(mapper);

                StatisticsMes entityCount = new StatisticsMes();
                entityCount.setName(entityName);
//                entityCount.setTableName(tableName);
                entityCount.setCount(count);
                counts.add(entityCount);
            }
        }

        return counts;
    }

    private Map<Class<?>, Object> getKnownEntityMappers() {
        Map<Class<?>, Object> entityMapperMap = new HashMap<>();

        // 手动映射已知的实体类和其对应的Mapper
        entityMapperMap.put(HttpRequestLog.class, httpRequestLogMapper);
        entityMapperMap.put(ScheduledLog.class, scheduledLogMapper);
        entityMapperMap.put(ScheduledConfig.class, scheduledConfigMapper);
        entityMapperMap.put(ThirdPartyHttpLog.class, thirdPartyHttpLogMapper);
        entityMapperMap.put(SystemRegister.class, systemRegisterMapper);

        return entityMapperMap;
    }

    private long getEntityCountFromMapper(Object mapper) {
        try {
            // 根据Mapper的具体类型调用相应的方法
            if (mapper instanceof HttpRequestLogMapper) {
                return ((HttpRequestLogMapper) mapper).selectCount(null);
            } else if (mapper instanceof ScheduledLogMapper) {
                return ((ScheduledLogMapper) mapper).selectCount(null);
            } else if (mapper instanceof ScheduledConfigMapper) {
                return ((ScheduledConfigMapper) mapper).selectCount(null);
            } else if (mapper instanceof ThirdPartyHttpLogMapper) {
                return ((ThirdPartyHttpLogMapper) mapper).selectCount(null);
            } else if (mapper instanceof SystemRegisterMapper) {
                return ((SystemRegisterMapper) mapper).selectCount(null);
            }
        } catch (Exception e) {
            System.err.println("获取实体计数失败: " + e.getMessage());
        }
        return 0;
    }

    private String getDefaultTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableName.class)) {
            com.baomidou.mybatisplus.annotation.TableName tableAnno =
                entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
            return tableAnno.value();
        }

        // 默认转换规则：类名转小写下划线格式
        String simpleName = entityClass.getSimpleName();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i != 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    @Override
    public List<StatisticsHour> getTodayHourlyThirdPartyCallStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        QueryWrapper<ThirdPartyHttpLog> wrapper = new QueryWrapper<>();
        wrapper.between("request_time", startOfDay, endOfDay);
        wrapper.eq("app_id", systemRegisterService.localSystem().getId());
        wrapper.select("FORMAT(request_time, '%H') as hour, count(*) as count")
                .groupBy("FORMAT(request_time, '%H')")
                .orderByAsc("hour");

        List<Map<String, Object>> result = thirdPartyHttpLogMapper.selectMaps(wrapper);

        // 初始化每小时数据为0
        Map<Integer, Long> hourCountMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCountMap.put(i, 0L);
        }

        // 填充实际数据
        for (Map<String, Object> item : result) {
            String hourStr = String.valueOf(item.get("hour"));
            Integer hour = Integer.parseInt(hourStr);
            Long count = Long.parseLong(String.valueOf(item.get("count")));
            hourCountMap.put(hour, count);
        }

        // 构建返回数据
        List<StatisticsHour> stats = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            StatisticsHour stat = new StatisticsHour();
            stat.setHour(String.format("%02d:00", i));
            stat.setCount(hourCountMap.get(i));
            stats.add(stat);
        }

        return stats;
    }

    @Override
    public StatisticsVO getFullStatistics() {
        StatisticsVO vo = new StatisticsVO();
        vo.setHourlyHttpRequestStats(getTodayHourlyHttpRequestStats());
        vo.setHourlyScheduledStats(getTodayHourlyScheduledStats());

        Map<String, Long> summary = getScheduledExecutionSummary();
        vo.setTotalScheduledExecutions(summary.get("total"));
        vo.setSuccessfulScheduledExecutions(summary.get("success"));

        vo.setCacheableEntityCounts(getCacheableEntityCounts());
        vo.setHourlyThirdPartyCallStats(getTodayHourlyThirdPartyCallStats());

        return vo;
    }
}
