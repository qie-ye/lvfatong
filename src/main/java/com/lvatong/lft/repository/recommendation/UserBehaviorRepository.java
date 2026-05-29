package com.lvatong.lft.repository.recommendation;

import com.lvatong.lft.model.entity.recommendation.UserBehavior;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    List<UserBehavior> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserBehavior> findByUserIdAndActionType(Long userId, UserBehavior.ActionType actionType);

    List<UserBehavior> findByUserIdAndTargetType(Long userId, UserBehavior.TargetType targetType);

    @Query("SELECT b FROM UserBehavior b WHERE b.userId = :userId AND b.createdAt >= :since ORDER BY b.createdAt DESC")
    List<UserBehavior> findRecentBehaviors(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT b.domain, COUNT(b) as cnt FROM UserBehavior b WHERE b.userId = :userId AND b.domain IS NOT NULL GROUP BY b.domain ORDER BY cnt DESC")
    List<Object[]> findTopDomainsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b.queryText, COUNT(b) as cnt FROM UserBehavior b WHERE b.queryText IS NOT NULL GROUP BY b.queryText ORDER BY cnt DESC")
    List<Object[]> findTopQueries(Pageable pageable);

    @Query("SELECT COUNT(b) FROM UserBehavior b WHERE b.userId = :userId AND b.createdAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT b.queryText, COUNT(b) as cnt FROM UserBehavior b WHERE b.queryText IS NOT NULL AND b.queryText != '' GROUP BY b.queryText ORDER BY cnt DESC")
    List<Object[]> findHotKeywords(@Param("limit") int limit);

    @Query("SELECT b.domain, COUNT(b) as cnt FROM UserBehavior b WHERE b.domain IS NOT NULL AND b.domain != '' GROUP BY b.domain ORDER BY cnt DESC")
    List<Object[]> findHotDomains(@Param("limit") int limit);
}
