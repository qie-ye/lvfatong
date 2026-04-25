package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.LegalOpinion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalOpinionRepository extends JpaRepository<LegalOpinion, Long> {
    List<LegalOpinion> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LegalOpinion> findByDomain(String domain);
}
