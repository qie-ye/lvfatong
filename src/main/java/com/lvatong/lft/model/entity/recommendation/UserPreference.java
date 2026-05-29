package com.lvatong.lft.model.entity.recommendation;

import com.lvatong.lft.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户偏好画像实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_user_preferences_user_id", columnList = "userId"),
    @Index(name = "idx_user_preferences_expertise", columnList = "expertiseLevel")
})
public class UserPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "preferred_domains", columnDefinition = "JSON")
    private String preferredDomains;

    @Column(name = "preferred_action_types", columnDefinition = "JSON")
    private String preferredActionTypes;

    @Column(name = "query_frequency")
    private Integer queryFrequency = 0;

    @Column(name = "avg_session_duration")
    private Integer avgSessionDuration = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "expertise_level", length = 20)
    private ExpertiseLevel expertiseLevel = ExpertiseLevel.BEGINNER;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    public enum ExpertiseLevel {
        BEGINNER,       // 初级
        INTERMEDIATE,   // 中级
        EXPERT          // 高级
    }
}
