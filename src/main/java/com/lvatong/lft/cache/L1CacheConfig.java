package com.lvatong.lft.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Data
@Configuration
@ConfigurationProperties(prefix = "lvatong.cache")
public class L1CacheConfig {

    private String spec = "maximumSize=1000,expireAfterWrite=10m";
    private CacheConfig ragResults = new CacheConfig(5, 500);
    private CacheConfig intentResults = new CacheConfig(10, 1000);
    private CacheConfig embeddingResults = new CacheConfig(30, 2000);

    @Data
    public static class CacheConfig {
        private long ttlMinutes;
        private int maxSize;

        public CacheConfig(long ttlMinutes, int maxSize) {
            this.ttlMinutes = ttlMinutes;
            this.maxSize = maxSize;
        }
    }

    @Bean(name = "ragCache")
    public Cache<String, Object> ragCache() {
        return Caffeine.newBuilder()
                .maximumSize(ragResults.getMaxSize())
                .expireAfterWrite(ragResults.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean(name = "intentCache")
    public Cache<String, Object> intentCache() {
        return Caffeine.newBuilder()
                .maximumSize(intentResults.getMaxSize())
                .expireAfterWrite(intentResults.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean(name = "embeddingCache")
    public Cache<String, Object> embeddingCache() {
        return Caffeine.newBuilder()
                .maximumSize(embeddingResults.getMaxSize())
                .expireAfterWrite(embeddingResults.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
