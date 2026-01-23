package com.uniops.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uniops.core.cache.EntityCacheManager;
import com.uniops.core.condition.UniEntityRequestCondition;
import com.uniops.core.entity.UniEntity;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.GenericEntityService;
import com.uniops.core.vo.UniEntityDataVO;
import com.uniops.core.vo.UniEntityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用实体管理Controller
 * 提供所有实体的CRUD操作接口
 */
@RestController
@RequestMapping("/business/entity")
@Tag(name = "通用实体管理", description = "提供所有实体的CRUD操作接口")
@Slf4j
public class GenericEntityController {

    @Autowired
    private GenericEntityService genericEntityService;

    @Autowired
    private EntityCacheManager entityCacheManager;

    /**
     * 获取所有注册的实体类名
     */
    @PostMapping("/listEntities")
    @Operation(summary = "获取所有实体类名", description = "获取系统中所有已注册的实体类名称列表")
    public Map<String, Object> listEntities() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", entityCacheManager.getAllEntityNames());
        return result;
    }

    /**
     * 获取实体表头信息
     */
    @PostMapping("/getTableHeader")
    @Operation(summary = "获取实体表头信息", description = "获取指定实体的表头字段信息")
    public ResponseResult<List<UniEntity>> getTableHeader(@RequestBody UniEntityRequestCondition condition) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = condition.getEntityName();
            List<UniEntity> headers = genericEntityService.getTableHeader(entityName);
            return ResponseResult.success(headers);
        } catch (Exception e) {
            return ResponseResult.error(e);
        }

    }

    /**
     * 分页查询表数据
     */
    @PostMapping("/getPageData")
    @Operation(summary = "分页查询实体数据", description = "根据实体名称和条件进行分页查询")
    public ResponseResult<UniEntityDataVO> getPageData(@RequestBody UniEntityRequestCondition condition) {
        String entityName = condition.getEntityName();
        int pageNum = condition.getPage();
        int pageSize = condition.getSize();
        Map<String, Object> conditions = condition.getConditions();

        // 构建查询条件
        QueryWrapper<Object> queryWrapper = new QueryWrapper<>();
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                queryWrapper.like(entry.getKey(), entry.getValue());
            }
        }

        IPage<?> page = genericEntityService.selectPage(entityName, pageNum, pageSize, queryWrapper);
        UniEntityDataVO uniEntityData = new UniEntityDataVO();
        uniEntityData.setRecords(page.getRecords());
        uniEntityData.setPage((int) page.getCurrent());
        uniEntityData.setSize((int) page.getSize());
        uniEntityData.setTotal(page.getTotal());
        uniEntityData.setTotalPages((int) page.getPages());
        return ResponseResult.success(uniEntityData);
    }

    // ... existing code ...

    /**
     * 新增实体
     */
    @PostMapping("/insert")
    @Operation(summary = "新增实体", description = "向指定实体表中插入新记录")
    public ResponseResult<Boolean> insert(@RequestBody UniEntityRequestCondition condition) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        String entityName = condition.getEntityName();
        Map<String, Object> entityData = condition.getData();

        // 将Map转换为实体对象（这里需要根据实际实体类进行转换）
        // 简单实现：使用反射创建对象并设置属性
        Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
        Object entity = entityClass.newInstance();

        for (Map.Entry<String, Object> entry : entityData.entrySet()) {
            Field field = entityClass.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            // 设置字段值，进行类型转换
            Object convertedValue = convertValueToFieldType(entry.getValue(), field.getType());
            field.set(entity, convertedValue);
        }

        boolean success = genericEntityService.insert(entityName, entity);

        return ResponseResult.success(success);
    }

    /**
     * 更新实体
     */
    @PostMapping("/update")
    @Operation(summary = "更新实体", description = "更新指定实体表中的记录")
    public ResponseResult<Boolean> update(@RequestBody UniEntityRequestCondition condition) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        String entityName = condition.getEntityName();
        Map<String, Object> entityData = condition.getData();

        // 将Map转换为实体对象
        Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
        Object entity = entityClass.newInstance();

        for (Map.Entry<String, Object> entry : entityData.entrySet()) {
            Field field = entityClass.getDeclaredField(entry.getKey());
            field.setAccessible(true);

            // 设置字段值，进行类型转换
            Object convertedValue = convertValueToFieldType(entry.getValue(), field.getType());
            field.set(entity, convertedValue);
        }

        boolean success = genericEntityService.update(entityName, entity);

        return ResponseResult.success(success);
    }
    // ... existing code ...

    /**
     * 将值转换为字段所需的类型
     */
    private Object convertValueToFieldType(Object value, Class<?> fieldType) {
        if (value == null) {
            return null;
        }

        // 如果类型已经匹配，直接返回
        if (fieldType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 处理日期类型
        if (fieldType == java.util.Date.class) {
            if (value instanceof String) {
                String dateStr = (String) value;
                // 如果是空字符串，返回null
                if (dateStr.trim().isEmpty()) {
                    return null;
                }
                try {
                    // 尝试解析日期字符串，支持多种格式
                    if (dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
                    } else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                    } else {
                        // 如果是时间戳格式
                        return new java.util.Date(Long.parseLong(dateStr));
                    }
                } catch (Exception e) {
                    log.warn("日期解析失败: " + value + ", 返回null", e);
                    return null;
                }
            } else if (value instanceof Number) {
                // 时间戳
                return new java.util.Date(((Number) value).longValue());
            }
            return null;
        } else if (fieldType == java.sql.Date.class) {
            if (value instanceof String) {
                String dateStr = (String) value;
                // 如果是空字符串，返回null
                if (dateStr.trim().isEmpty()) {
                    return null;
                }
                try {
                    return java.sql.Date.valueOf(dateStr);
                } catch (Exception e) {
                    log.warn("SQL日期解析失败: " + value + ", 返回null", e);
                    return null;
                }
            } else if (value instanceof java.util.Date) {
                return new java.sql.Date(((java.util.Date) value).getTime());
            } else if (value instanceof Number) {
                return new java.sql.Date(((Number) value).longValue());
            }
            return null;
        } else if (fieldType == java.sql.Timestamp.class) {
            if (value instanceof String) {
                String dateStr = (String) value;
                // 如果是空字符串，返回null
                if (dateStr.trim().isEmpty()) {
                    return null;
                }
                try {
                    if (dateStr.contains(" ")) {
                        return java.sql.Timestamp.valueOf(dateStr);
                    } else {
                        return new java.sql.Timestamp(new java.util.Date(Long.parseLong(dateStr)).getTime());
                    }
                } catch (Exception e) {
                    log.warn("时间戳解析失败: " + value + ", 返回null", e);
                    return null;
                }
            } else if (value instanceof java.util.Date) {
                return new java.sql.Timestamp(((java.util.Date) value).getTime());
            } else if (value instanceof Number) {
                return new java.sql.Timestamp(((Number) value).longValue());
            }
            return null;
        }

        // 处理基本数据类型及其包装类之间的转换
        if (fieldType == Long.class || fieldType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Long.valueOf(strValue);
            }
        } else if (fieldType == Integer.class || fieldType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Integer.valueOf(strValue);
            }
        } else if (fieldType == Short.class || fieldType == short.class) {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Short.valueOf(strValue);
            }
        } else if (fieldType == Byte.class || fieldType == byte.class) {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Byte.valueOf(strValue);
            }
        } else if (fieldType == Float.class || fieldType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Float.valueOf(strValue);
            }
        } else if (fieldType == Double.class || fieldType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回null
                if (strValue.trim().isEmpty()) {
                    return null;
                }
                return Double.valueOf(strValue);
            }
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                String strValue = value.toString();
                // 如果是空字符串，返回false
                if (strValue.trim().isEmpty()) {
                    return false;
                }
                return Boolean.valueOf(strValue);
            }
        } else if (fieldType == String.class) {
            return value.toString();
        } else if (fieldType == Character.class || fieldType == char.class) {
            String str = value.toString();
            return str.length() > 0 ? str.charAt(0) : '\0';
        }

        // 如果都处理不了，就返回原值
        return value;
    }
    // ... existing code ...


    /**
     * 删除实体
     */
    @PostMapping("/delete")
    @Operation(summary = "删除实体", description = "根据ID删除指定实体表中的记录")
    public ResponseResult<Boolean> delete(@RequestBody UniEntityRequestCondition condition) {
        String entityName = condition.getEntityName();
        Map<String, Object> entityData = condition.getData();

        // 获取实体类信息以确定主键字段
        Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
        if (entityClass == null) {
            return ResponseResult.error("未找到实体 " + entityName + " 对应的Class");
        }

        // 获取主键字段名
        String primaryKeyField = getPrimaryKeyFieldName(entityClass);
        if (primaryKeyField == null || !entityData.containsKey(primaryKeyField)) {
            return ResponseResult.error("未找到主键字段或主键值");
        }

        // 从entityData中获取主键值
        Object id = entityData.get(primaryKeyField);

        boolean success = genericEntityService.delete(entityName, id);
        return ResponseResult.success(success);
    }

    /**
     * 获取主键字段名
     */
    private String getPrimaryKeyFieldName(Class<?> entityClass) {
        try {
            // 查找主键字段（优先查找@TableId注解，其次查找名为id的字段）
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableId.class)) {
                    return field.getName(); // 返回主键字段名
                }
            }

            // 如果没有找到@TableId注解，尝试查找名为id的字段
            try {
                java.lang.reflect.Field idField = entityClass.getDeclaredField("id");
                if (idField != null) {
                    return "id"; // 返回默认主键字段名
                }
            } catch (NoSuchFieldException e) {
                // 忽略，继续
            }
        } catch (Exception e) {
            log.error("获取主键字段名失败: ", e);
        }
        return null;
    }

    /**
     * 查询单个实体
     */
    @PostMapping("/selectById")
    @Operation(summary = "根据ID查询实体", description = "根据ID查询指定实体表中的记录")
    public Map<String, Object> selectById(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = (String) request.get("entityName");
            Object id = request.get("id");

            Object entity = genericEntityService.selectById(entityName, id);

            result.put("success", true);
            result.put("data", entity);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 刷新实体缓存
     */
    @PostMapping("/refreshCache")
    @Operation(summary = "刷新实体缓存", description = "刷新指定实体的缓存")
    public Map<String, Object> refreshCache(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = request.get("entityName");
            entityCacheManager.refreshCache(entityName);

            result.put("success", true);
            result.put("message", "缓存刷新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 清空所有缓存
     */
    @PostMapping("/clearAllCache")
    @Operation(summary = "清空所有缓存", description = "清空实体管理器中的所有缓存")
    public Map<String, Object> clearAllCache() {
        Map<String, Object> result = new HashMap<>();
        try {
            entityCacheManager.clearAllCache();

            result.put("success", true);
            result.put("message", "所有缓存已清空");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
