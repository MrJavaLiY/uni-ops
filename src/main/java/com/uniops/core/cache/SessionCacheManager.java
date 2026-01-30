// src/main/java/com/uniops/core/cache/SessionCacheManager.java
package com.uniops.core.cache;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话缓存管理器
 * 用于缓存会话信息，减少数据库访问
 */
@Component
public class SessionCacheManager {

    // 会话缓存：sessionToken -> (lastCheckTime, isValid)
    private final ConcurrentHashMap<String, SessionCacheEntry> sessionCache = new ConcurrentHashMap<>();

    private static final long CACHE_DURATION_SECONDS = 300; // 缓存5分钟

    /**
     * 会话缓存条目
     */
    private static class SessionCacheEntry {
        private final boolean isValid;
        private final LocalDateTime cacheTime;

        public SessionCacheEntry(boolean isValid) {
            this.isValid = isValid;
            this.cacheTime = LocalDateTime.now();
        }

        public boolean isValid() {
            return isValid;
        }

        public LocalDateTime getCacheTime() {
            return cacheTime;
        }

        public boolean isExpired() {
            return java.time.Duration.between(cacheTime, LocalDateTime.now()).getSeconds() > CACHE_DURATION_SECONDS;
        }
    }

    /**
     * 获取缓存的会话有效性
     */
    public Boolean getCachedSessionValidity(String sessionToken) {
        SessionCacheEntry entry = sessionCache.get(sessionToken);
        if (entry == null || entry.isExpired()) {
            sessionCache.remove(sessionToken);
            return null;
        }
        return entry.isValid();
    }

    /**
     * 缓存会话有效性
     */
    public void cacheSessionValidity(String sessionToken, boolean isValid) {
        sessionCache.put(sessionToken, new SessionCacheEntry(isValid));
    }

    /**
     * 移除缓存
     */
    public void removeCachedSession(String sessionToken) {
        sessionCache.remove(sessionToken);
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        sessionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
