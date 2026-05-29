package com.lvatong.lft.model.entity.recommendation;

import com.lvatong.lft.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户行为记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_behaviors", indexes = {
    @Index(name = "idx_user_behaviors_user_id", columnList = "userId"),
    @Index(name = "idx_user_behaviors_action_type", columnList = "actionType"),
    @Index(name = "idx_user_behaviors_target_type", columnList = "targetType"),
    @Index(name = "idx_user_behaviors_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_behaviors_user_action", columnList = "userId, actionType"),
    @Index(name = "idx_user_behaviors_domain", columnList = "domain")
})
public class UserBehavior extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "query_text", length = 500)
    private String queryText;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "rating", length = 20)
    private String rating;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    public enum ActionType {
        SEARCH,     // 搜索
        VIEW,       // 查看
        CLICK,      // 点击
        FEEDBACK,   // 反馈
        SHARE,      // 分享
        BOOKMARK    // 收藏
    }

    public enum TargetType {
        LAW,        // 法条
        CASE,       // 案例
        FAQ,        // FAQ
        LAWYER,     // 律师
        CONTRACT,   // 合同
        CHAT,       // 聊天
        OPINION,    // 法律意见
        DOCUMENT    // 法律文书
    }
}
