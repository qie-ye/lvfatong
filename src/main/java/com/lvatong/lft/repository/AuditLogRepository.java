package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
