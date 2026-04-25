package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 200)
    private String resource;

    @Column(length = 500)
    private String detail;

    @Column(length = 50)
    private String ip;

    @Column(length = 10)
    private String method;

    @Column(length = 200)
    private String uri;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
