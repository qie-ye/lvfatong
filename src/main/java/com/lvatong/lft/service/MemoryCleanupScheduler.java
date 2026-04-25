package com.lvatong.lft.service;

import com.lvatong.lft.model.entity.ChatSession;
import com.lvatong.lft.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryCleanupScheduler {

    private final ChatSessionRepository chatSessionRepository;
    private final SessionSummaryService sessionSummaryService;
    private final UserMemoryService userMemoryService;

    /**
     * 每10分钟扫描无摘要的已结束会话，补生成摘要
     */
    @Scheduled(fixedRate = 600_000)
    public void generateMissingSummaries() {
        try {
            List<ChatSession> endedWithoutSummary = chatSessionRepository.findEndedWithoutSummary();
            if (endedWithoutSummary.isEmpty()) return;

            log.info("Found {} ended sessions without summary, generating...", endedWithoutSummary.size());
            for (ChatSession session : endedWithoutSummary) {
                try {
                    sessionSummaryService.generateSummary(session.getId(), session.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to generate summary for session {}: {}", session.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Memory cleanup scheduler error: {}", e.getMessage());
        }
    }

    /**
     * 每小时清理超出容量限制的旧记忆
     */
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupOldMemories() {
        try {
            // Get distinct user IDs from ended sessions
            List<ChatSession> endedSessions = chatSessionRepository.findByStatus("ENDED");
            if (endedSessions == null || endedSessions.isEmpty()) return;

            endedSessions.stream()
                    .map(ChatSession::getUserId)
                    .distinct()
                    .forEach(userId -> {
                        try {
                            userMemoryService.cleanupOldMemories(userId);
                        } catch (Exception e) {
                            log.warn("Memory cleanup failed for user {}: {}", userId, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Memory cleanup scheduler error: {}", e.getMessage());
        }
    }
}
