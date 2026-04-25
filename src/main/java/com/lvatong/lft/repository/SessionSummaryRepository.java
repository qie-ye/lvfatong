package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.SessionSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionSummaryRepository extends JpaRepository<SessionSummary, Long> {

    Optional<SessionSummary> findBySessionId(Long sessionId);

    List<SessionSummary> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    void deleteBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);
}
