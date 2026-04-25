package com.lvatong.lft.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.model.entity.SessionSummary;
import com.lvatong.lft.model.entity.UserMemory;
import com.lvatong.lft.repository.SessionSummaryRepository;
import com.lvatong.lft.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryService {

    private final UserMemoryRepository userMemoryRepository;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_SUMMARIES_PER_USER = 20;
    private static final int MAX_MEMORIES_PER_USER = 50;

    /**
     * 根据当前问题检索相关历史摘要和用户画像
     */
    public String getRelevantMemories(Long userId, String currentQuestion) {
        try {
            StringBuilder context = new StringBuilder();

            // L3: User memories (preferences, topics, profile)
            List<UserMemory> memories = userMemoryRepository
                    .findByUserIdAndMemoryTypeIn(userId,
                            List.of(UserMemory.MemoryType.TOPIC, UserMemory.MemoryType.PREFERENCE, UserMemory.MemoryType.PROFILE));

            if (!memories.isEmpty()) {
                for (UserMemory mem : memories) {
                    context.append("- ").append(mem.getMemoryType().name()).append("/")
                            .append(mem.getKey()).append("：").append(mem.getValue()).append("\n");
                }
            }

            // L2: Relevant session summaries (top 3 by topic overlap)
            List<SessionSummary> summaries = findRelevantSummaries(userId, currentQuestion, 3);
            if (!summaries.isEmpty()) {
                context.append("\n近期相关对话摘要：\n");
                for (SessionSummary s : summaries) {
                    context.append("- ").append(s.getSummary()).append("\n");
                }
            }

            return context.isEmpty() ? null : context.toString();
        } catch (Exception e) {
            log.warn("Failed to get relevant memories for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 构建注入system prompt的记忆上下文
     */
    public String buildMemoryContext(Long userId, String question) {
        String memories = getRelevantMemories(userId, question);
        if (memories == null || memories.isBlank()) return null;
        return "[用户历史记忆]\n" + memories;
    }

    /**
     * 查看用户所有记忆（API用）
     */
    public List<UserMemory> getUserMemories(Long userId) {
        return userMemoryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 删除单条记忆（API用）
     */
    @Transactional
    public boolean deleteUserMemory(Long userId, Long memoryId) {
        try {
            userMemoryRepository.deleteByIdAndUserId(memoryId, userId);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete memory {} for user {}: {}", memoryId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * 清理超出容量限制的旧记忆
     */
    @Transactional
    public void cleanupOldMemories(Long userId) {
        // Cleanup summaries
        long summaryCount = sessionSummaryRepository.countByUserId(userId);
        if (summaryCount > MAX_SUMMARIES_PER_USER) {
            List<SessionSummary> summaries = sessionSummaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
            int toRemove = (int) (summaryCount - MAX_SUMMARIES_PER_USER);
            for (int i = summaries.size() - 1; i >= 0 && toRemove > 0; i--, toRemove--) {
                sessionSummaryRepository.delete(summaries.get(i));
            }
            log.info("Cleaned up {} old summaries for user {}", toRemove, userId);
        }

        // Cleanup user memories
        long memoryCount = userMemoryRepository.countByUserId(userId);
        if (memoryCount > MAX_MEMORIES_PER_USER) {
            List<UserMemory> memories = userMemoryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
            int toRemove = (int) (memoryCount - MAX_MEMORIES_PER_USER);
            for (int i = memories.size() - 1; i >= 0 && toRemove > 0; i--, toRemove--) {
                userMemoryRepository.delete(memories.get(i));
            }
            log.info("Cleaned up {} old memories for user {}", toRemove, userId);
        }
    }

    /**
     * 按topics关键词交集匹配，取topN相关摘要
     */
    private List<SessionSummary> findRelevantSummaries(Long userId, String question, int topN) {
        List<SessionSummary> allSummaries = sessionSummaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (allSummaries.isEmpty()) return Collections.emptyList();

        // Extract keywords from current question
        Set<String> questionKeywords = extractKeywords(question);

        // Score each summary by topic overlap
        List<ScoredSummary> scored = new ArrayList<>();
        for (SessionSummary s : allSummaries) {
            Set<String> summaryTopics = parseJsonArray(s.getTopics());
            long overlap = questionKeywords.stream().filter(summaryTopics::contains).count();
            if (overlap > 0) {
                scored.add(new ScoredSummary(s, overlap));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingLong(ScoredSummary::score).reversed())
                .limit(topN)
                .map(ScoredSummary::summary)
                .collect(Collectors.toList());
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        // Simple keyword extraction: split by common delimiters and filter short tokens
        return Arrays.stream(text.split("[\\s，。、？！；：\u201c\u201d\u2018\u2019\uff08\uff09\\[\\]{}]+"))
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());
    }

    private Set<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptySet();
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private record ScoredSummary(SessionSummary summary, long score) {}
}
