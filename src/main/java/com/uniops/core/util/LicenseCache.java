package com.uniops.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LicenseCache 类的简要描述
 *
 * @author liyang
 * @since 2026/1/21
 */
public class LicenseCache {
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final Map<String, Long> expirationMap = new ConcurrentHashMap<>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 将键值对放入缓存，默认不过期
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public static void put(String key, Object value) {
        lock.writeLock().lock();
        try {
            cache.put(key, value);
            expirationMap.remove(key); // 清除过期时间
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 将键值对放入缓存，指定过期时间（毫秒）
     *
     * @param key         缓存键
     * @param value       缓存值
     * @param expireTime 过期时间（毫秒）
     */
    public static void put(String key, Object value, long expireTime) {
        lock.writeLock().lock();
        try {
            cache.put(key, value);
            expirationMap.put(key, System.currentTimeMillis() + expireTime);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从缓存中获取值
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    public static Object get(String key) {
        lock.readLock().lock();
        try {
            // 检查是否已过期
            Long expireTime = expirationMap.get(key);
            if (expireTime != null && System.currentTimeMillis() > expireTime) {
                // 已过期，删除缓存项
                remove(key);
                return null;
            }

            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 从缓存中获取值并转换为指定类型
     *
     * @param key          缓存键
     * @param expectedType 期望的类型
     * @param <T>          泛型类型
     * @return 缓存值，如果不存在或已过期则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> expectedType) {
        Object value = get(key);
        if (value != null && expectedType.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 检查缓存中是否存在指定键
     *
     * @param key 缓存键
     * @return 如果存在且未过期返回true，否则返回false
     */
    public static boolean containsKey(String key) {
        lock.readLock().lock();
        try {
            if (!cache.containsKey(key)) {
                return false;
            }

            // 检查是否已过期
            Long expireTime = expirationMap.get(key);
            if (expireTime != null && System.currentTimeMillis() > expireTime) {
                // 已过期，删除缓存项
                remove(key);
                return false;
            }

            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 删除指定键的缓存项
     *
     * @param key 缓存键
     * @return 被删除的值，如果不存在返回null
     */
    public static Object remove(String key) {
        lock.writeLock().lock();
        try {
            Object value = cache.remove(key);
            expirationMap.remove(key);
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空所有缓存项
     */
    public static void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            expirationMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存项数量
     */
    public static int size() {
        lock.readLock().lock();
        try {
            // 清理过期的项
            cleanupExpired();
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清理过期的缓存项
     */
    public static void cleanupExpired() {
        lock.writeLock().lock();
        try {
            expirationMap.entrySet().removeIf(entry -> {
                if (System.currentTimeMillis() > entry.getValue()) {
                    cache.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查缓存是否为空
     *
     * @return 如果缓存为空返回true，否则返回false
     */
    public static boolean isEmpty() {
        return size() == 0;
    }
}
