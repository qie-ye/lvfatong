package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.LawyerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LawyerReviewRepository extends JpaRepository<LawyerReview, Long> {
    Optional<LawyerReview> findByUserIdAndLawyerId(Long userId, Long lawyerId);
    List<LawyerReview> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);
    List<LawyerReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT AVG(r.rating) FROM LawyerReview r WHERE r.lawyerId = ?1")
    Double getAverageRating(Long lawyerId);

    @Query("SELECT COUNT(r) FROM LawyerReview r WHERE r.lawyerId = ?1")
    Long getReviewCount(Long lawyerId);

    @Query("SELECT r.lawyerId, AVG(r.rating) as avgRating, COUNT(r) as reviewCount FROM LawyerReview r GROUP BY r.lawyerId ORDER BY avgRating DESC")
    List<Object[]> getLawyerRatingsRanked();

    // Find lawyers that similar users (who rated the same lawyer highly) also rated highly
    @Query("SELECT r2.lawyerId, AVG(r2.rating) as score FROM LawyerReview r1 " +
           "JOIN LawyerReview r2 ON r1.lawyerId <> r2.lawyerId " +
           "WHERE r1.userId = ?1 AND r1.rating >= 4 AND r2.userId IN " +
           "(SELECT r3.userId FROM LawyerReview r3 WHERE r3.lawyerId = r1.lawyerId AND r3.rating >= 4 AND r3.userId <> ?1) " +
           "GROUP BY r2.lawyerId ORDER BY score DESC")
    List<Object[]> findCollaborativeRecommendations(Long userId);
}
