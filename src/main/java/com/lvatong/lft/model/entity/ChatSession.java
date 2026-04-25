package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_sessions")
public class ChatSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SessionType type;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    public enum SessionType {
        LEGAL_QA, CONTRACT_ANALYSIS
    }

    public enum SessionStatus {
        ACTIVE, ENDED
    }
}
