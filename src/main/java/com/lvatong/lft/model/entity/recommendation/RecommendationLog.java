package com.lvatong.lft.model.entity.recommendation;

import com.lvatong.lft.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 推荐记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recommendation_logs", indexes = {
    @Index(name = "idx_recommendation_logs_user_id", columnList = "userId"),
    @Index(name = "idx_recommendation_logs_type", columnList = "recommendationType"),
    @Index(name = "idx_recommendation_logs_created_at", columnList = "createdAt")
})
public class RecommendationLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 50)
    private RecommendationType recommendationType;

    @Column(name = "recommended_items", nullable = false, columnDefinition = "JSON")
    private String recommendedItems;

    @Column(name = "algorithm_version", length = 20)
    private String algorithmVersion = "v1";

    @Column(name = "clicked")
    private Boolean clicked = false;

    @Column(name = "clicked_item_id")
    private Long clickedItemId;

    @Column(name = "feedback_rating", length = 20)
    private String feedbackRating;

    public enum RecommendationType {
        LAW,        // 法条推荐
        CASE,       // 案例推荐
        FAQ,        // FAQ推荐
        LAWYER,     // 律师推荐
        TOPIC       // 话题推荐
    }
}
