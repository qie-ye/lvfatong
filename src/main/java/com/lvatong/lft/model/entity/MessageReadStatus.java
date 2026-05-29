package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "message_read_status", indexes = {
        @Index(name = "idx_message_read_status_user_id", columnList = "userId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_user", columnNames = {"message_id", "user_id"})
})
public class MessageReadStatus extends BaseEntity {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}