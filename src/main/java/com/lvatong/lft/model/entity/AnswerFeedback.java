package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "answer_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_feedback_user_session_msg",
                columnNames = {"user_id", "session_id", "message_index"}))
public class AnswerFeedback extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "message_index", nullable = false)
    private Integer messageIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Rating rating;

    public enum Rating {
        GOOD, BAD
    }
}
