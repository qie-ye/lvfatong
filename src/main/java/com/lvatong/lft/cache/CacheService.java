package com.lvatong.lft.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    @Qualifier("ragCache")
    private final Cache<String, Object> ragCache;

    @Qualifier("intentCache")
    private final Cache<String, Object> intentCache;

    @Qualifier("embeddingCache")
    private final Cache<String, Object> embeddingCache;

    // ==================== RAG Cache ====================

    /**
     * Get cached RAG result
     */
    @SuppressWarnings("unchecked")
    public <T> T getRagResult(String key) {
        return (T) ragCache.getIfPresent(key);
    }

    /**
     * Cache RAG result
     */
    public void putRagResult(String key, Object value) {
        ragCache.put(key, value);
        log.debug("Cached RAG result: key={}", key);
    }

    /**
     * Get or compute RAG result
     */
    @SuppressWarnings("unchecked")
    public <T> T getRagResult(String key, java.util.function.Supplier<T> supplier) {
        return (T) ragCache.get(key, k -> {
            log.debug("RAG cache miss: key={}", k);
            return supplier.get();
        });
    }

    // ==================== Intent Cache ====================

    /**
     * Get cached intent result
     */
    @SuppressWarnings("unchecked")
    public <T> T getIntentResult(String key) {
        return (T) intentCache.getIfPresent(key);
    }

    /**
     * Cache intent result
     */
    public void putIntentResult(String key, Object value) {
        intentCache.put(key, value);
        log.debug("Cached intent result: key={}", key);
    }

    /**
     * Get or compute intent result
     */
    @SuppressWarnings("unchecked")
    public <T> T getIntentResult(String key, java.util.function.Supplier<T> supplier) {
        return (T) intentCache.get(key, k -> {
            log.debug("Intent cache miss: key={}", k);
            return supplier.get();
        });
    }

    // ==================== Embedding Cache ====================

    /**
     * Get cached embedding
     */
    @SuppressWarnings("unchecked")
    public List<Float> getEmbedding(String key) {
        return (List<Float>) embeddingCache.getIfPresent(key);
    }

    /**
     * Cache embedding
     */
    public void putEmbedding(String key, List<Float> embedding) {
        embeddingCache.put(key, embedding);
        log.debug("Cached embedding: key={}", key);
    }

    /**
     * Get or compute embedding
     */
    @SuppressWarnings("unchecked")
    public List<Float> getEmbedding(String key, java.util.function.Supplier<List<Float>> supplier) {
        return (List<Float>) embeddingCache.get(key, k -> {
            log.debug("Embedding cache miss: key={}", k);
            return supplier.get();
        });
    }

    // ==================== Cache Management ====================

    /**
     * Clear all caches
     */
    public void clearAll() {
        ragCache.invalidateAll();
        intentCache.invalidateAll();
        embeddingCache.invalidateAll();
        log.info("All L1 caches cleared");
    }

    /**
     * Clear specific cache
     */
    public void clearCache(CacheType type) {
        switch (type) {
            case RAG -> ragCache.invalidateAll();
            case INTENT -> intentCache.invalidateAll();
            case EMBEDDING -> embeddingCache.invalidateAll();
        }
        log.info("Cache cleared: {}", type);
    }

    /**
     * Get cache stats
     */
    public CacheStats getStats(CacheType type) {
        return switch (type) {
            case RAG -> ragCache.stats();
            case INTENT -> intentCache.stats();
            case EMBEDDING -> embeddingCache.stats();
        };
    }

    /**
     * Get cache size
     */
    public long getSize(CacheType type) {
        return switch (type) {
            case RAG -> ragCache.estimatedSize();
            case INTENT -> intentCache.estimatedSize();
            case EMBEDDING -> embeddingCache.estimatedSize();
        };
    }

    /**
     * Cache type enum
     */
    public enum CacheType {
        RAG,
        INTENT,
        EMBEDDING
    }
}
