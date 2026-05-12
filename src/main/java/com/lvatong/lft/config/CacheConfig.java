package com.lvatong.lft.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            // 测试 Redis 连接是否可用
            try (var conn = connectionFactory.getConnection()) {
                conn.ping();
            }
            log.info("Redis 缓存已启用");
        } catch (Exception e) {
            log.warn("Redis 不可用（{}），降级为内存缓存", e.getMessage());
            return new ConcurrentMapCacheManager(
                    "faqCache", "lawSearchCache", "lawyerListCache",
                    "caseSearchCache", "contractTemplatesCache",
                    "opinionCache", "chatHistoryCache"
            );
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer redisSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("faqCache",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("lawSearchCache",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("lawyerListCache",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("caseSearchCache",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("contractTemplatesCache",
                        defaultConfig.entryTtl(Duration.ofHours(6)))
                .build();
    }
}
