package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamId(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.teamId = :teamId")
    long countByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT tm.userId FROM TeamMember tm WHERE tm.teamId = :teamId")
    List<Long> findUserIdsByTeamId(@Param("teamId") Long teamId);
}