package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.CaseCollaboration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseCollaborationRepository extends JpaRepository<CaseCollaboration, Long> {

    List<CaseCollaboration> findByTeamId(Long teamId);

    List<CaseCollaboration> findByCaseId(Long caseId);

    Optional<CaseCollaboration> findByCaseIdAndTeamId(Long caseId, Long teamId);

    boolean existsByCaseIdAndTeamId(Long caseId, Long teamId);
}