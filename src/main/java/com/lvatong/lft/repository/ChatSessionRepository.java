package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(DISTINCT s.userId) FROM ChatSession s WHERE s.createdAt >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    List<ChatSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<ChatSession> findByStatus(String status);

    @Query("SELECT s FROM ChatSession s WHERE s.status = 'ENDED' AND s.id NOT IN (SELECT ss.sessionId FROM SessionSummary ss)")
    List<ChatSession> findEndedWithoutSummary();
}
