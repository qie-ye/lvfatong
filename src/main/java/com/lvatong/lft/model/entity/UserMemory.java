package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_memories")
public class UserMemory extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 20)
    private MemoryType memoryType;

    @Column(name = "`key`", nullable = false, length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "source_session_id")
    private Long sourceSessionId;

    @Column
    private Double confidence;

    public enum MemoryType {
        PREFERENCE,  // 用户偏好
        TOPIC,       // 关注领域
        PROFILE      // 用户画像
    }
}
