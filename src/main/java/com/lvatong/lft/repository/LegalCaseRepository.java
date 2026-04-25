package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.LegalCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LegalCaseRepository extends JpaRepository<LegalCase, Long> {

    Page<LegalCase> findByCaseType(String caseType, Pageable pageable);

    Page<LegalCase> findByDomain(String domain, Pageable pageable);

    Page<LegalCase> findByCourtContaining(String court, Pageable pageable);

    Page<LegalCase> findByYear(String year, Pageable pageable);

    @Query("SELECT lc FROM LegalCase lc WHERE lc.title LIKE %:keyword% OR lc.keywords LIKE %:keyword% OR lc.summary LIKE %:keyword%")
    Page<LegalCase> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT lc FROM LegalCase lc WHERE lc.domain = :domain AND lc.caseType = :caseType")
    Page<LegalCase> findByDomainAndCaseType(@Param("domain") String domain, @Param("caseType") String caseType, Pageable pageable);

    List<LegalCase> findByVectorIndexedFalse();

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM legal_cases WHERE MATCH(summary, facts, ruling) AGAINST(:query IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    Page<LegalCase> fullTextSearch(@Param("query") String query, Pageable pageable);
}
