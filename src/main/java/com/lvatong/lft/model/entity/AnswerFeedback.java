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

    /**
     * 用户问题（用于微调数据）
     */
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    /**
     * AI回答（用于微调数据）
     */
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    /**
     * 意图类型（LEGAL_QA, LAW_QUERY, CONTRACT_QUESTION等）
     */
    @Column(name = "intent_type", length = 50)
    private String intentType;

    /**
     * 使用的模型（glm-4-flash, glm-4-plus等）
     */
    @Column(name = "model_used", length = 50)
    private String modelUsed;

    /**
     * 检索的上下文（用于分析检索质量）
     */
    @Column(name = "context_used", columnDefinition = "TEXT")
    private String contextUsed;

    /**
     * 点踩原因（可选，用户填写）
     */
    @Column(name = "bad_reason", length = 500)
    private String badReason;

    /**
     * 问题分类标签（用于优先级排序）
     * 例如：法条错误、逻辑混乱、回答不完整、幻觉等
     */
    @Column(name = "issue_tags", length = 200)
    private String issueTags;

    public enum Rating {
        GOOD, BAD
    }
}
