package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    List<TeamInvitation> findByTeamIdAndStatus(Long teamId, TeamInvitation.InvitationStatus status);

    List<TeamInvitation> findByInviteeIdAndStatus(Long inviteeId, TeamInvitation.InvitationStatus status);

    Optional<TeamInvitation> findByTeamIdAndInviteeIdAndStatus(Long teamId, Long inviteeId, TeamInvitation.InvitationStatus status);

    boolean existsByTeamIdAndInviteeIdAndStatus(Long teamId, Long inviteeId, TeamInvitation.InvitationStatus status);
}