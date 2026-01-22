package com.uniops.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uniops.core.cache.EntityCacheManager;
import com.uniops.core.condition.UniEntityRequestCondition;
import com.uniops.core.entity.UniEntity;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.GenericEntityService;
import com.uniops.core.vo.UniEntityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public Map<String, Object> getPageData(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = (String) request.get("entityName");
            int pageNum = (Integer) request.getOrDefault("pageNum", 1);
            int pageSize = (Integer) request.getOrDefault("pageSize", 10);
            Map<String, Object> conditions = (Map<String, Object>) request.getOrDefault("conditions", new HashMap<>());

            // 构建查询条件
            QueryWrapper<Object> queryWrapper = new QueryWrapper<>();
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                    queryWrapper.like(entry.getKey(), entry.getValue());
                }
            }

            IPage<?> page = genericEntityService.selectPage(entityName, pageNum, pageSize, queryWrapper);

            result.put("success", true);
            result.put("data", page.getRecords());
            result.put("total", page.getTotal());
            result.put("pages", page.getPages());
            result.put("current", page.getCurrent());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 新增实体
     */
    @PostMapping("/insert")
    @Operation(summary = "新增实体", description = "向指定实体表中插入新记录")
    public Map<String, Object> insert(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = (String) request.get("entityName");
            Map<String, Object> entityData = (Map<String, Object>) request.get("entity");

            // 将Map转换为实体对象（这里需要根据实际实体类进行转换）
            // 简单实现：使用反射创建对象并设置属性
            Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
            Object entity = entityClass.newInstance();

            for (Map.Entry<String, Object> entry : entityData.entrySet()) {
                Field field = entityClass.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(entity, entry.getValue());
            }

            boolean success = genericEntityService.insert(entityName, entity);

            result.put("success", success);
            result.put("message", success ? "新增成功" : "新增失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "新增失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 更新实体
     */
    @PostMapping("/update")
    @Operation(summary = "更新实体", description = "更新指定实体表中的记录")
    public Map<String, Object> update(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = (String) request.get("entityName");
            Map<String, Object> entityData = (Map<String, Object>) request.get("entity");

            // 将Map转换为实体对象
            Class<?> entityClass = entityCacheManager.getEntityClass(entityName);
            Object entity = entityClass.newInstance();

            for (Map.Entry<String, Object> entry : entityData.entrySet()) {
                Field field = entityClass.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(entity, entry.getValue());
            }

            boolean success = genericEntityService.update(entityName, entity);

            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 删除实体
     */
    @PostMapping("/delete")
    @Operation(summary = "删除实体", description = "根据ID删除指定实体表中的记录")
    public Map<String, Object> delete(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String entityName = (String) request.get("entityName");
            Object id = request.get("id");

            boolean success = genericEntityService.delete(entityName, id);

            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
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
