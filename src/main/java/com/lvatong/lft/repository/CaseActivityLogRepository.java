package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.CaseActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseActivityLogRepository extends JpaRepository<CaseActivityLog, Long> {

    List<CaseActivityLog> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<CaseActivityLog> findTop20ByCaseIdOrderByCreatedAtDesc(Long caseId);
}