package com.lvatong.lft.service;

import com.lvatong.lft.ai.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatService chatService;

    private static final String SESSION_PREFIX = "chat:session:";
    private static final int MAX_ROUNDS = 10;
    private static final long SESSION_TIMEOUT_MINUTES = 120;

    /** GLM-4 系列上下文窗口 128K tokens */
    private static final int CONTEXT_WINDOW_TOKENS = 128_000;
    /** 中文平均 1 字 ≈ 1.5 tokens */
    private static final double CHARS_PER_TOKEN = 0.67;
    /** 压缩触发阈值（上下文用量超过 70% 时触发） */
    private static final double COMPRESS_THRESHOLD = 0.7;

    /**
     * 添加消息到会话历史（滑动窗口10轮）
     */
    public void addMessage(Long sessionId, String role, String content) {
        String key = SESSION_PREFIX + sessionId;
        try {
            Map<String, String> message = new LinkedHashMap<>();
            message.put("role", role);
            message.put("content", content);
            redisTemplate.opsForList().rightPush(key, message);
            redisTemplate.expire(key, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_ROUNDS * 2) {
                redisTemplate.opsForList().trim(key, size - MAX_ROUNDS * 2, -1);
            }
        } catch (Exception e) {
            log.warn("Redis session storage failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 获取会话历史（用于构建对话上下文）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getSessionHistory(Long sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null) return Collections.emptyList();
            List<Map<String, String>> history = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map) {
                    history.add((Map<String, String>) item);
                }
            }
            return history;
        } catch (Exception e) {
            log.warn("Redis session read failed for session {}: {}", sessionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 刷新会话超时
     */
    public void refreshSession(Long sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            redisTemplate.expire(key, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("Redis session refresh failed");
        }
    }

    /**
     * 清除会话
     */
    public void clearSession(Long sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis session clear failed");
        }
    }

    /**
     * 预热会话：将L2/L3相关记忆注入Redis作为预热上下文
     */
    public void warmupSession(Long sessionId, List<Map<String, String>> memories) {
        if (memories == null || memories.isEmpty()) return;
        String key = SESSION_PREFIX + sessionId;
        try {
            for (Map<String, String> mem : memories) {
                redisTemplate.opsForList().rightPush(key, mem);
            }
            redisTemplate.expire(key, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            log.debug("Warmed up session {} with {} memory entries", sessionId, memories.size());
        } catch (Exception e) {
            log.warn("Redis session warmup failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 结束会话：标记Redis中的会话为已结束状态
     */
    public void endSession(Long sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            // Set a short TTL so it expires soon but allows any in-flight request to complete
            redisTemplate.expire(key, 30, TimeUnit.SECONDS);
            log.debug("Session {} marked for ending", sessionId);
        } catch (Exception e) {
            log.debug("Redis session end failed");
        }
    }

    // ==================== 上下文用量 & 压缩 ====================

    /**
     * 估算当前会话的上下文 token 用量
     */
    public ContextUsage estimateContextUsage(Long sessionId) {
        List<Map<String, String>> history = getSessionHistory(sessionId);
        int totalChars = 0;
        for (Map<String, String> msg : history) {
            String content = msg.get("content");
            if (content != null) totalChars += content.length();
        }
        int estimatedTokens = (int) (totalChars / CHARS_PER_TOKEN);
        double usageRatio = (double) estimatedTokens / CONTEXT_WINDOW_TOKENS;
        boolean needCompress = usageRatio >= COMPRESS_THRESHOLD;
        return new ContextUsage(estimatedTokens, CONTEXT_WINDOW_TOKENS, usageRatio, needCompress, history.size());
    }

    /**
     * 压缩上下文：将旧消息摘要为一条 system 消息，保留最近 N 轮
     */
    @SuppressWarnings("unchecked")
    public boolean compressContext(Long sessionId) {
        String key = SESSION_PREFIX + sessionId;
        try {
            List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null || raw.size() <= 4) return false;  // 太少无需压缩

            List<Map<String, String>> allMessages = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map) allMessages.add((Map<String, String>) item);
            }

            // 保留最近 4 条（2轮），其余摘要
            int keepRecent = Math.min(4, allMessages.size());
            List<Map<String, String>> oldMessages = allMessages.subList(0, allMessages.size() - keepRecent);
            List<Map<String, String>> recentMessages = allMessages.subList(allMessages.size() - keepRecent, allMessages.size());

            if (oldMessages.isEmpty()) return false;

            // 构建摘要 prompt
            StringBuilder sb = new StringBuilder("请用200字以内概括以下对话的关键信息和结论：\n\n");
            for (Map<String, String> msg : oldMessages) {
                String role = msg.getOrDefault("role", "user");
                String content = msg.getOrDefault("content", "");
                sb.append(role).append(": ").append(content, 0, Math.min(content.length(), 300)).append("\n");
            }

            String summary = chatService.simpleChat(sb.toString(), "glm-4-flash", 0.3, 256);

            // 重建 Redis 列表：摘要 + 最近消息
            redisTemplate.delete(key);
            Map<String, String> summaryMsg = new LinkedHashMap<>();
            summaryMsg.put("role", "system");
            summaryMsg.put("content", "[历史对话摘要] " + summary);
            redisTemplate.opsForList().rightPush(key, summaryMsg);
            for (Map<String, String> msg : recentMessages) {
                redisTemplate.opsForList().rightPush(key, msg);
            }
            redisTemplate.expire(key, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            log.info("Context compressed for session {}: {} messages → summary + {} recent",
                    sessionId, allMessages.size(), keepRecent);
            return true;
        } catch (Exception e) {
            log.warn("Context compression failed for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * 上下文用量 DTO
     */
    public record ContextUsage(
            int usedTokens,
            int totalTokens,
            double usageRatio,
            boolean needCompress,
            int messageCount
    ) {}
}
