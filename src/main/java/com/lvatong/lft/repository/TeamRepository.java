package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByOwnerIdAndStatus(Long ownerId, Team.TeamStatus status);

    Optional<Team> findByInviteCode(String inviteCode);

    @Query("SELECT t FROM Team t JOIN TeamMember tm ON t.id = tm.teamId WHERE tm.userId = :userId AND t.status = 'ACTIVE'")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}