package com.lvatong.lft.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.model.entity.ChatMessage;
import com.lvatong.lft.model.entity.SessionSummary;
import com.lvatong.lft.model.entity.UserMemory;
import com.lvatong.lft.repository.ChatMessageRepository;
import com.lvatong.lft.repository.SessionSummaryRepository;
import com.lvatong.lft.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    private final ChatMessageRepository chatMessageRepository;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final UserMemoryRepository userMemoryRepository;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    private final ExecutorService summaryExecutor = Executors.newFixedThreadPool(2);

    @PreDestroy
    public void shutdown() {
        summaryExecutor.shutdown();
        try {
            if (!summaryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                summaryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            summaryExecutor.shutdownNow();
        }
    }

    private static final String SUMMARY_PROMPT = """
            请分析以下法律咨询对话，生成结构化摘要。
            只返回如下JSON，不要解释：
            {
              "summary": "对话摘要（50字以内）",
              "topics": ["主题1", "主题2"],
              "key_points": ["要点1", "要点2"],
              "preferences": {"领域": "偏好值", "风格": "偏好值"}
            }
            
            对话内容：
            %s
            """;

    /**
     * 同步生成会话摘要
     */
    @Transactional
    public SessionSummary generateSummary(Long sessionId, Long userId) {
        if (sessionSummaryRepository.existsBySessionId(sessionId)) {
            log.debug("Summary already exists for session {}", sessionId);
            return sessionSummaryRepository.findBySessionId(sessionId).orElse(null);
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (messages.isEmpty()) {
            log.debug("No messages for session {}, skipping summary", sessionId);
            return null;
        }

        try {
            String conversationText = buildConversationText(messages);
            String prompt = SUMMARY_PROMPT.formatted(
                    conversationText.length() > 4000 ? conversationText.substring(0, 4000) : conversationText);

            String llmResponse = chatService.simpleChat(prompt, "glm-4-flash", 0.3, 1024);
            Map<String, Object> parsed = parseSummaryJson(llmResponse);

            SessionSummary summary = new SessionSummary();
            summary.setSessionId(sessionId);
            summary.setUserId(userId);
            summary.setSummary((String) parsed.getOrDefault("summary", ""));
            summary.setTopics(toJsonString(parsed.get("topics")));
            summary.setKeyPoints(toJsonString(parsed.get("key_points")));
            summary.setMessageCount(messages.size());

            summary = sessionSummaryRepository.save(summary);
            log.info("Summary generated for session {}, topics={}", sessionId, parsed.get("topics"));

            // Extract user memories from the summary
            extractUserMemories(sessionId, userId, parsed);

            return summary;
        } catch (Exception e) {
            log.warn("Failed to generate summary for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * 异步生成会话摘要
     */
    @Async
    public void generateSummaryAsync(Long sessionId, Long userId) {
        summaryExecutor.execute(() -> {
            try {
                generateSummary(sessionId, userId);
            } catch (Exception e) {
                log.warn("Async summary generation failed for session {}: {}", sessionId, e.getMessage());
            }
        });
    }

    /**
     * 从摘要中提取用户偏好/关注领域，更新user_memories表
     */
    @Transactional
    public void extractUserMemories(Long sessionId, Long userId, Map<String, Object> parsed) {
        try {
            // Extract topics as TOPIC memories
            Object topicsObj = parsed.get("topics");
            if (topicsObj instanceof List<?> topics) {
                String topicValue = String.join("、", topics.stream().map(Object::toString).toList());
                upsertMemory(userId, UserMemory.MemoryType.TOPIC, "关注领域", topicValue, sessionId, 0.7);
            }

            // Extract preferences
            Object prefsObj = parsed.get("preferences");
            if (prefsObj instanceof Map<?, ?> prefs) {
                for (Map.Entry<?, ?> entry : prefs.entrySet()) {
                    String key = entry.getKey().toString();
                    String value = entry.getValue().toString();
                    upsertMemory(userId, UserMemory.MemoryType.PREFERENCE, key, value, sessionId, 0.6);
                }
            }

            // Extract key points as PROFILE
            Object keyPointsObj = parsed.get("key_points");
            if (keyPointsObj instanceof List<?> keyPoints && !keyPoints.isEmpty()) {
                String profileValue = String.join("；", keyPoints.stream().map(Object::toString).toList());
                upsertMemory(userId, UserMemory.MemoryType.PROFILE, "近期咨询", profileValue, sessionId, 0.7);
            }
        } catch (Exception e) {
            log.warn("Failed to extract user memories from session {}: {}", sessionId, e.getMessage());
        }
    }

    private void upsertMemory(Long userId, UserMemory.MemoryType type, String key,
                              String value, Long sourceSessionId, double confidence) {
        Optional<UserMemory> existing = userMemoryRepository.findByUserIdAndMemoryTypeAndKey(userId, type, key);
        if (existing.isPresent()) {
            UserMemory mem = existing.get();
            mem.setValue(value);
            mem.setSourceSessionId(sourceSessionId);
            mem.setConfidence(confidence);
            userMemoryRepository.save(mem);
        } else {
            UserMemory mem = new UserMemory();
            mem.setUserId(userId);
            mem.setMemoryType(type);
            mem.setKey(key);
            mem.setValue(value);
            mem.setSourceSessionId(sourceSessionId);
            mem.setConfidence(confidence);
            userMemoryRepository.save(mem);
        }
    }

    private String buildConversationText(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            sb.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSummaryJson(String llmResponse) {
        try {
            // Try to extract JSON from the response (may have markdown wrapping)
            String json = llmResponse.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse summary JSON: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summary", llmResponse.length() > 100 ? llmResponse.substring(0, 100) : llmResponse);
            fallback.put("topics", Collections.emptyList());
            fallback.put("key_points", Collections.emptyList());
            return fallback;
        }
    }

    private String toJsonString(Object obj) {
        try {
            if (obj == null) return null;
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
