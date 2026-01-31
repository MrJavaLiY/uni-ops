package com.uniops.core.cache;

import com.uniops.core.annotation.ManageEntity;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体缓存管理器
 * 负责扫描所有带有@CacheableEntity注解的类，并构建缓存
 */
@Component
public class EntityCacheManager implements ApplicationContextAware {

    @Autowired
    private org.apache.ibatis.session.SqlSession sqlSession;

    private ApplicationContext applicationContext;

    // 存储所有实体类的缓存：实体类名 -> 缓存Map（主键 -> 实体对象）
    private final Map<String, Map<Object, Object>> entityCaches = new ConcurrentHashMap<>();

    // 存储实体类信息：实体类名 -> 实体类Class
    private final Map<String, Class<?>> entityClasses = new ConcurrentHashMap<>();

    // 存储实体类对应的Mapper：实体类名 -> Mapper接口
    private final Map<String, Object> entityMappers = new ConcurrentHashMap<>();

    // 扫描的包路径 - 修改为实际的项目包路径
    private static final String BASE_PACKAGE = "com.uniops.core.entity";

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {
        scanAndBuildCache();
    }

    /**
     * 扫描所有带有@CacheableEntity注解的类，并构建缓存
     */
    private void scanAndBuildCache() throws IOException, ClassNotFoundException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);

        // 扫描指定包下的所有类
        String packagePath = BASE_PACKAGE.replace('.', '/');
        Resource[] resources = resolver.getResources("classpath*:" + packagePath + "/**/*.class");

        for (Resource resource : resources) {
            try {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());

                // 检查是否有@CacheableEntity注解
                if (clazz.isAnnotationPresent(ManageEntity.class)) {
                    ManageEntity annotation = clazz.getAnnotation(ManageEntity.class);
                    String entityName = annotation.value();
                    if (entityName.isEmpty()) {
                        entityName = clazz.getSimpleName();
                    }

                    // 存储实体类信息
                    entityClasses.put(entityName, clazz);

                    // 创建缓存Map
                    entityCaches.put(entityName, new ConcurrentHashMap<>());

                    // 获取对应的Mapper（假设Mapper接口命名规则为：实体类名 + Mapper）
                    String mapperName = className.replace("entity", "mapper") + "Mapper";
                    try {
                        Class<?> mapperClass = ClassUtils.forName(mapperName, ClassUtils.getDefaultClassLoader());
                        // 从Spring上下文获取实际的Mapper Bean
                        Object mapperBean = getMapperBean(mapperClass);
                        if (mapperBean != null) {
                            entityMappers.put(entityName, mapperBean);
                            System.out.println("成功注册实体类: " + entityName + " -> " + className + ", 对应Mapper: " + mapperName);
                        } else {
                            System.err.println("未找到对应的Mapper Bean: " + mapperName);
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("未找到对应的Mapper: " + mapperName);
                    }
                }
            } catch (Exception e) {
                System.err.println("扫描类失败: " + e.getMessage());
            }
        }

        // 如果没有找到任何带@CacheableEntity注解的类，则扫描所有实体类
        if (entityClasses.isEmpty()) {
            System.out.println("未找到带@CacheableEntity注解的实体类，开始扫描所有实体类...");
            for (Resource resource : resources) {
                try {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());

                    // 检查是否为实体类（通常继承自Model或包含@Table注解等）
                    if (isEntityClass(clazz)) {
                        String entityName = clazz.getSimpleName();

                        // 存储实体类信息
                        entityClasses.put(entityName, clazz);

                        // 创建缓存Map
                        entityCaches.put(entityName, new ConcurrentHashMap<>());

                        // 获取对应的Mapper
                        String mapperName = className.replace("entity", "mapper") + "Mapper";
                        try {
                            Class<?> mapperClass = ClassUtils.forName(mapperName, ClassUtils.getDefaultClassLoader());
                            // 从Spring上下文获取实际的Mapper Bean
                            Object mapperBean = getMapperBean(mapperClass);
                            if (mapperBean != null) {
                                entityMappers.put(entityName, mapperBean);
                                System.out.println("发现实体类: " + entityName + " -> " + className + ", 对应Mapper: " + mapperName);
                            } else {
                                System.out.println("未找到对应的Mapper Bean: " + mapperName + "，但仍注册实体类: " + entityName);
                            }
                        } catch (ClassNotFoundException e) {
                            System.out.println("未找到对应的Mapper: " + mapperName + "，但仍注册实体类: " + entityName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("扫描类失败: " + e.getMessage());
                }
            }
        }

        System.out.println("实体缓存管理器初始化完成，共注册 " + entityClasses.size() + " 个实体类");
    }

    /**
     * 从Spring上下文中获取Mapper Bean
     */
    private Object getMapperBean(Class<?> mapperClass) {
        try {
            // 尝试按类型获取Bean
            String[] beanNames = applicationContext.getBeanNamesForType(mapperClass);
            if (beanNames.length > 0) {
                return applicationContext.getBean(mapperClass);
            }
        } catch (Exception e) {
            System.err.println("获取Mapper Bean失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否为实体类
     */
    private boolean isEntityClass(Class<?> clazz) {
        // 检查是否包含常见的实体类注解如@Entity、@TableName等
        return
               clazz.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableName.class) ||
               // 如果没有特定注解，可以根据类名后缀判断
               clazz.getSimpleName().endsWith("Entity") ||
               clazz.getSimpleName().endsWith("Info") ||
               clazz.getSimpleName().endsWith("Record") ||
               // 或者判断是否在实体包下且不是接口也不是抽象类
               (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()));
    }

    /**
     * 获取实体类的缓存
     */
    public Map<Object, Object> getEntityCache(String entityName) {
        return entityCaches.get(entityName);
    }

    /**
     * 获取实体类对应的Mapper
     */
    public Object getMapper(String entityName) {
        return entityMappers.get(entityName);
    }

    /**
     * 获取实体类的Class对象
     */
    public Class<?> getEntityClass(String entityName) {
        return entityClasses.get(entityName);
    }

    /**
     * 获取所有注册的实体类名
     */
    public Set<String> getAllEntityNames() {
        return entityClasses.keySet();
    }

    /**
     * 刷新指定实体的缓存（从数据库重新加载）
     */
    public void refreshCache(String entityName) {
        Map<Object, Object> cache = entityCaches.get(entityName);
        if (cache != null) {
            cache.clear();
            // 这里可以添加从数据库加载所有数据的逻辑
            System.out.println("刷新实体缓存: " + entityName);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        entityCaches.values().forEach(Map::clear);
        System.out.println("清空所有实体缓存");
    }
}
