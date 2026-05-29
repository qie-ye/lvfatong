package com.lvatong.lft.repository.recommendation;

import com.lvatong.lft.model.entity.recommendation.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {

    List<RecommendationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM RecommendationLog r WHERE r.userId = :userId AND r.clicked = true ORDER BY r.createdAt DESC")
    List<RecommendationLog> findClickedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RecommendationLog r WHERE r.userId = :userId AND r.clicked = true")
    long countClickedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RecommendationLog r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT r.recommendationType, AVG(CASE WHEN r.clicked = true THEN 1.0 ELSE 0.0 END) as ctr FROM RecommendationLog r WHERE r.createdAt >= :since GROUP BY r.recommendationType")
    List<Object[]> calculateCTRByType(@Param("since") LocalDateTime since);

    long countByClicked(boolean clicked);

    @Query("SELECT COUNT(r) FROM RecommendationLog r WHERE r.feedbackRating IS NOT NULL")
    long countByFeedbackRatingIsNotNull();
}
