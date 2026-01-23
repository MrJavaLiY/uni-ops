package com.uniops.core.service;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniops.core.cache.EntityCacheManager;
import com.uniops.core.entity.UniEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 通用实体服务类
 * 提供针对所有注册实体的CRUD操作
 */
@Service
@Slf4j
public class GenericEntityService {

    @Autowired
    private EntityCacheManager entityCacheManager;

    /**
     * 查询单个实体
     */
    public Object selectById(String entityName, Object id) {
        // 先从缓存中查找
        Map<Object, Object> cache = entityCacheManager.getEntityCache(entityName);
        if (cache != null && cache.containsKey(id)) {
            return cache.get(id);
        }

        // 从数据库查询
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            // 使用反射调用selectById方法
            Method method = mapper.getClass().getMethod("selectById", Object.class);
            Object result = method.invoke(mapper, id);

            if (result != null && cache != null) {
                cache.put(id, result);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询实体失败: " + e.getMessage(), e);
        }
    }

    // ... existing code ...

    /**
     * 分页查询
     */
    public IPage<?> selectPage(String entityName, int pageNum, int pageSize, QueryWrapper<?> queryWrapper) {
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            // 获取实体类信息以确定主键字段
            Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
            if (entityClass == null) {
                throw new RuntimeException("未找到实体 " + entityName + " 对应的Class");
            }

            // 获取主键字段名
            String primaryKeyField = getPrimaryKeyFieldName(entityClass);
            if (primaryKeyField != null && !primaryKeyField.isEmpty()) {
                // 如果QueryWrapper中没有排序条件，则按主键排序
                if (queryWrapper == null) {
                    queryWrapper = new QueryWrapper<>();
                }

                // 检查QueryWrapper是否已经有ORDER BY子句，如果没有则添加主键排序
                try {
                    // 尝试获取当前QueryWrapper的排序信息，如果为空则设置默认排序
                    // 通过反射检查是否存在排序条件
                    java.lang.reflect.Field ordersField = queryWrapper.getClass().getDeclaredField("orders");
                    ordersField.setAccessible(true);
                    List ordersList = (List) ordersField.get(queryWrapper);

                    if (ordersList == null || ordersList.isEmpty()) {
                        queryWrapper.orderByAsc(primaryKeyField); // 默认按主键升序排列
                    }
                } catch (Exception e) {
                    // 如果反射获取排序信息失败，也设置默认排序
                    queryWrapper.orderByAsc(primaryKeyField);
                }
            }

            // 获取Mapper接口的所有方法，寻找分页查询方法
            Method selectPageMethod = null;
            Method[] methods = mapper.getClass().getMethods(); // 获取所有公共方法

            for (Method method : methods) {
                if ("selectPage".equals(method.getName())) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 2) {
                        selectPageMethod = method;
                        break;
                    }
                }
            }

            if (selectPageMethod == null) {
                // 如果没有找到selectPage方法，尝试使用selectList进行分页
                // 获取总记录数
                Method selectCountMethod = null;
                for (Method method : methods) {
                    if ("selectCount".equals(method.getName())) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == 1 &&
                                QueryWrapper.class.isAssignableFrom(paramTypes[0])) {
                            selectCountMethod = method;
                            break;
                        }
                    }
                }

                if (selectCountMethod != null) {
                    // 使用selectList和手动分页
                    Method selectListMethod = null;
                    for (Method method : methods) {
                        if ("selectList".equals(method.getName())) {
                            Class<?>[] paramTypes = method.getParameterTypes();
                            if (paramTypes.length == 1 &&
                                    QueryWrapper.class.isAssignableFrom(paramTypes[0])) {
                                selectListMethod = method;
                                break;
                            }
                        }
                    }

                    if (selectListMethod != null) {
                        // 执行查询所有符合条件的数据
                        List<?> allResults = (List<?>) selectListMethod.invoke(mapper, queryWrapper);

                        // 计算分页结果
                        int startIndex = (pageNum - 1) * pageSize;
                        int endIndex = Math.min(startIndex + pageSize, allResults.size());

                        if (startIndex >= allResults.size()) {
                            // 超出范围，返回空页面
                            Page<?> emptyPage = new Page<>(pageNum, pageSize);
                            emptyPage.setRecords(Collections.emptyList());
                            emptyPage.setTotal(0);
                            emptyPage.setPages(0);
                            return emptyPage;
                        }

                        List pageRecords = allResults.subList(startIndex, endIndex);

                        Page<?> resultPage = new Page<>(pageNum, pageSize);
                        resultPage.setRecords(pageRecords);
                        resultPage.setTotal(allResults.size());
                        resultPage.setPages((int) Math.ceil((double) allResults.size() / pageSize));

                        return resultPage;
                    }
                }

                throw new RuntimeException("未找到合适的分页查询方法");
            }

            Page<?> page = new Page<>(pageNum, pageSize);
            return (IPage<?>) selectPageMethod.invoke(mapper, page, queryWrapper);
        } catch (Exception e) {
            log.error("分页查询失败: ", e);
            throw new RuntimeException("分页查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取主键字段名
     */
    private String getPrimaryKeyFieldName(Class<?> entityClass) {
        try {
            // 查找主键字段（优先查找@TableId注解，其次查找名为id的字段）
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableId.class)) {
                    return field.getName(); // 返回主键字段名
                }
            }

            // 如果没有找到@TableId注解，尝试查找名为id的字段
            try {
                Field idField = entityClass.getDeclaredField("id");
                if (idField != null) {
                    return "id"; // 返回默认主键字段名
                }
            } catch (NoSuchFieldException e) {
                // 忽略，继续
            }
        } catch (Exception e) {
            System.err.println("获取主键字段名失败: " + e.getMessage());
        }
        return null;
    }
    // ... existing code ...


    /**
     * 新增实体
     */
    public boolean insert(String entityName, Object entity) {
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            Method method = mapper.getClass().getMethod("insert", Object.class);
            Integer result = (Integer) method.invoke(mapper, entity);

            if (result > 0) {
                // 获取主键值并更新缓存
                Object id = getPrimaryKeyValue(entity, entityName);
                if (id != null) {
                    Map<Object, Object> cache = entityCacheManager.getEntityCache(entityName);
                    if (cache != null) {
                        cache.put(id, entity);
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("新增实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新实体
     */
    public boolean update(String entityName, Object entity) {
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            Method method = mapper.getClass().getMethod("updateById", Object.class);
            Integer result = (Integer) method.invoke(mapper, entity);

            if (result > 0) {
                // 更新缓存
                Object id = getPrimaryKeyValue(entity, entityName);
                if (id != null) {
                    Map<Object, Object> cache = entityCacheManager.getEntityCache(entityName);
                    if (cache != null) {
                        cache.put(id, entity);
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("更新实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除实体
     */
    public boolean delete(String entityName, Object id) {
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            Method method = mapper.getClass().getMethod("deleteById", Object.class);
            Integer result = (Integer) method.invoke(mapper, id);

            if (result > 0) {
                // 清除缓存
                Map<Object, Object> cache = entityCacheManager.getEntityCache(entityName);
                if (cache != null) {
                    cache.remove(id);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("删除实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取实体表头信息
     */
    public List<UniEntity> getTableHeader(String entityName) {
        Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
        if (entityClass == null) {
            throw new RuntimeException("未找到实体 " + entityName);
        }

        List<UniEntity> headers = new ArrayList<>();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            UniEntity header = new UniEntity();
            if (field.isAnnotationPresent(TableId.class)) {
                //说明这是主键
                header.setPrimaryKey(true);
//                header.setColumnName(field.getAnnotation(TableId.class).value());
                header.setPrimaryType(field.getAnnotation(TableId.class).type().name());
            } else if (field.isAnnotationPresent(TableField.class)) {
                if (!field.getAnnotation(TableField.class).exist()) {
                    continue;
                }
//                else {
//                    header.setColumnName(field.getAnnotation(TableField.class).value());
//                }
            }
            if (StringUtils.isEmpty(header.getColumnName())) {
                header.setColumnName(field.getName());
            }
            header.setColumnType(getFieldType(field));
            headers.add(header);
        }

        return headers;
    }

    /**
     * 获取实体的所有数据（用于缓存预热）
     */
    public List<?> selectAll(String entityName) {
        Object mapper = entityCacheManager.getMapper(entityName);
        if (mapper == null) {
            throw new RuntimeException("未找到实体 " + entityName + " 对应的Mapper");
        }

        try {
            Method method = mapper.getClass().getMethod("selectList", QueryWrapper.class);
            return (List<?>) method.invoke(mapper, new QueryWrapper<>());
        } catch (Exception e) {
            throw new RuntimeException("查询所有实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取主键值
     */
    private Object getPrimaryKeyValue(Object entity, String entityName) {
        try {
            Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
            if (entityClass == null) return null;

            // 查找主键字段（优先查找@TableId注解，其次查找名为id的字段）
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableId.class)) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }

            // 如果没有找到@TableId注解，尝试查找名为id的字段
            Field idField = entityClass.getDeclaredField("id");
            if (idField != null) {
                idField.setAccessible(true);
                return idField.get(entity);
            }
        } catch (Exception e) {
            System.err.println("获取主键值失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取字段显示名称
     */
    private String getFieldDisplayName(Field field) {
        // 这里可以根据实际需求添加注解或配置来获取更友好的显示名称
        // 简单实现：将字段名转换为驼峰转下划线形式
        String name = field.getName();
        StringBuilder result = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        return result.toString().trim();
    }

    /**
     * 获取字段类型
     */
    private String getFieldType(Field field) {
        Class<?> type = field.getType();
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class) return "number";
        if (type == Long.class || type == long.class) return "number";
        if (type == Double.class || type == double.class) return "number";
        if (type == Date.class) return "date";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        return "string";
    }
}
