package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.FaqEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findByEnabledTrueOrderByIdAsc();

    List<FaqEntry> findByCategoryAndEnabledTrue(String category);

    List<FaqEntry> findByCategory(String category, Pageable pageable);

    @Query("SELECT f FROM FaqEntry f WHERE f.enabled = true AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%)")
    List<FaqEntry> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT f.* FROM faq_entries f " +
            "WHERE f.enabled = TRUE " +
            "AND MATCH(f.question) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
            "LIMIT :limit", nativeQuery = true)
    List<FaqEntry> fulltextSearch(@Param("query") String query, @Param("limit") int limit);

    @Query(value = "SELECT f.*, MATCH(f.question) AGAINST(:query IN NATURAL LANGUAGE MODE) AS score " +
            "FROM faq_entries f " +
            "WHERE f.enabled = TRUE " +
            "AND MATCH(f.question) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
            "ORDER BY score DESC " +
            "LIMIT 1", nativeQuery = true)
    Optional<Object[]> fulltextSearchTopWithScore(@Param("query") String query);

    @Query("SELECT DISTINCT f.category FROM FaqEntry f WHERE f.enabled = true AND f.category IS NOT NULL ORDER BY f.category")
    List<String> findAllCategories();
}
