package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, Long> {
    List<ContractDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT c.status, COUNT(c) FROM ContractDocument c GROUP BY c.status")
    List<Object[]> countByStatusGroup();

    @Query("SELECT FUNCTION('DATE', c.createdAt), COUNT(c) FROM ContractDocument c WHERE c.createdAt >= :since GROUP BY FUNCTION('DATE', c.createdAt) ORDER BY FUNCTION('DATE', c.createdAt)")
    List<Object[]> countByDaySince(@Param("since") LocalDateTime since);
}
