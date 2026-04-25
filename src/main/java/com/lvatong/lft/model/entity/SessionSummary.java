package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "session_summaries")
public class SessionSummary extends BaseEntity {

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "JSON")
    private String topics;

    @Column(name = "key_points", columnDefinition = "JSON")
    private String keyPoints;

    @Column(name = "message_count")
    private Integer messageCount;
}
