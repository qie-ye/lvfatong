package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.TeamFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamFileRepository extends JpaRepository<TeamFile, Long> {

    List<TeamFile> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    List<TeamFile> findByTeamIdAndCaseIdOrderByCreatedAtDesc(Long teamId, Long caseId);

    long countByTeamId(Long teamId);

    @Query("SELECT COALESCE(SUM(t.fileSize), 0) FROM TeamFile t WHERE t.teamId = :teamId")
    long sumFileSizeByTeamId(@Param("teamId") Long teamId);
}