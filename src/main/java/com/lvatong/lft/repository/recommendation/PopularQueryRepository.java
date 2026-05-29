package com.lvatong.lft.repository.recommendation;

import com.lvatong.lft.model.entity.recommendation.PopularQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PopularQueryRepository extends JpaRepository<PopularQuery, Long> {

    Optional<PopularQuery> findByQueryText(String queryText);

    @Query("SELECT p FROM PopularQuery p ORDER BY p.queryCount DESC")
    List<PopularQuery> findTopQueries(Pageable pageable);

    @Query("SELECT p FROM PopularQuery p WHERE p.domain = :domain ORDER BY p.queryCount DESC")
    List<PopularQuery> findTopQueriesByDomain(@Param("domain") String domain, Pageable pageable);

    @Modifying
    @Query("UPDATE PopularQuery p SET p.queryCount = p.queryCount + 1, p.lastQueriedAt = CURRENT_TIMESTAMP WHERE p.queryText = :queryText")
    int incrementQueryCount(@Param("queryText") String queryText);
}
