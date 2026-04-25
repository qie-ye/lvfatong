package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.AnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerFeedbackRepository extends JpaRepository<AnswerFeedback, Long> {

    Optional<AnswerFeedback> findByUserIdAndSessionIdAndMessageIndex(
            Long userId, Long sessionId, Integer messageIndex);

    long countByRating(AnswerFeedback.Rating rating);

    @Query("SELECT CAST(f.createdAt AS date) AS day, " +
           "SUM(CASE WHEN f.rating = 'GOOD' THEN 1 ELSE 0 END) AS goodCount, " +
           "COUNT(f) AS total " +
           "FROM AnswerFeedback f " +
           "GROUP BY CAST(f.createdAt AS date) " +
           "ORDER BY CAST(f.createdAt AS date) DESC")
    List<Object[]> findDailyStats();
}
