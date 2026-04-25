package com.lvatong.lft.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ZhipuApiClient zhipuApiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "embedding:";
    private static final long CACHE_TTL_HOURS = 24;

    /**
     * 生成单个文本的embedding（带Redis缓存）
     */
    @SuppressWarnings("unchecked")
    public List<Float> embed(String text) {
        String cacheKey = CACHE_PREFIX + hashText(text);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<Float>) cached;
            }
        } catch (Exception e) {
            log.debug("Redis cache read failed, fallback to API call");
        }

        List<Float> embedding = zhipuApiClient.embed(text);

        try {
            redisTemplate.opsForValue().set(cacheKey, embedding, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("Redis cache write failed");
        }

        return embedding;
    }

    /**
     * 批量生成embedding
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        return zhipuApiClient.embedBatch(texts);
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
