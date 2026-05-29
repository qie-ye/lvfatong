package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_user_id", columnList = "userId"),
        @Index(name = "idx_comments_parent_id", columnList = "parentId")
})
public class Comment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    public enum TargetType {
        CASE,       // 案件
        TASK,       // 任务
        DOCUMENT    // 文档
    }
}