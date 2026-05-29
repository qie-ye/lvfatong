package com.lvatong.lft.team;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Team;
import com.lvatong.lft.model.entity.TeamInvitation;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.TeamInvitationRepository;
import com.lvatong.lft.repository.TeamMemberRepository;
import com.lvatong.lft.repository.TeamRepository;
import com.lvatong.lft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    /**
     * 邀请成员（通过手机号）
     */
    @Transactional
    public TeamInvitation inviteByPhone(Long teamId, Long inviterId, String phone) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("团队不存在"));

        // 检查邀请者权限
        TeamMember.MemberRole inviterRole = teamMemberRepository.findByTeamIdAndUserId(teamId, inviterId)
                .map(TeamMember::getRole)
                .orElse(null);
        if (inviterRole != TeamMember.MemberRole.OWNER && inviterRole != TeamMember.MemberRole.ADMIN) {
            throw new BusinessException("只有所有者或管理员可以邀请成员");
        }

        // 查找被邀请用户
        Optional<User> inviteeOpt = userRepository.findByPhone(phone);
        if (inviteeOpt.isPresent()) {
            User invitee = inviteeOpt.get();
            
            // 检查是否已是成员
            if (teamMemberRepository.existsByTeamIdAndUserId(teamId, invitee.getId())) {
                throw new BusinessException("该用户已是团队成员");
            }

            // 检查是否已有待处理的邀请
            if (teamInvitationRepository.existsByTeamIdAndInviteeIdAndStatus(teamId, invitee.getId(), TeamInvitation.InvitationStatus.PENDING)) {
                throw new BusinessException("已向该用户发送过邀请");
            }

            // 检查成员数量
            long memberCount = teamMemberRepository.countByTeamId(teamId);
            if (memberCount >= team.getMaxMembers()) {
                throw new BusinessException("团队成员已满");
            }

            // 创建邀请
            TeamInvitation invitation = new TeamInvitation();
            invitation.setTeamId(teamId);
            invitation.setInviterId(inviterId);
            invitation.setInviteeId(invitee.getId());
            invitation.setInviteePhone(phone);
            invitation.setStatus(TeamInvitation.InvitationStatus.PENDING);
            invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

            invitation = teamInvitationRepository.save(invitation);
            log.info("邀请已发送: teamId={}, inviteeId={}", teamId, invitee.getId());
            return invitation;
        } else {
            // 被邀请用户不存在，记录手机号邀请
            TeamInvitation invitation = new TeamInvitation();
            invitation.setTeamId(teamId);
            invitation.setInviterId(inviterId);
            invitation.setInviteePhone(phone);
            invitation.setStatus(TeamInvitation.InvitationStatus.PENDING);
            invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

            invitation = teamInvitationRepository.save(invitation);
            log.info("邀请已发送（手机号）: teamId={}, phone={}", teamId, phone);
            return invitation;
        }
    }

    /**
     * 获取团队的邀请列表
     */
    public List<TeamInvitation> getTeamInvitations(Long teamId) {
        return teamInvitationRepository.findByTeamIdAndStatus(teamId, TeamInvitation.InvitationStatus.PENDING);
    }

    /**
     * 获取用户收到的邀请列表
     */
    public List<TeamInvitation> getUserInvitations(Long userId) {
        return teamInvitationRepository.findByInviteeIdAndStatus(userId, TeamInvitation.InvitationStatus.PENDING);
    }

    /**
     * 接受邀请
     */
    @Transactional
    public Team acceptInvitation(Long invitationId, Long userId) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));

        // 检查邀请状态
        if (invitation.getStatus() != TeamInvitation.InvitationStatus.PENDING) {
            throw new BusinessException("邀请已处理");
        }

        // 检查是否过期
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(TeamInvitation.InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BusinessException("邀请已过期");
        }

        // 检查被邀请人
        if (invitation.getInviteeId() == null || !invitation.getInviteeId().equals(userId)) {
            throw new BusinessException("您不是被邀请人");
        }

        Team team = teamRepository.findById(invitation.getTeamId())
                .orElseThrow(() -> new BusinessException("团队不存在"));

        // 检查是否已是成员
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            invitation.setStatus(TeamInvitation.InvitationStatus.ACCEPTED);
            teamInvitationRepository.save(invitation);
            throw new BusinessException("您已是团队成员");
        }

        // 检查成员数量
        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        if (memberCount >= team.getMaxMembers()) {
            throw new BusinessException("团队成员已满");
        }

        // 添加成员
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(TeamMember.MemberRole.MEMBER);
        teamMemberRepository.save(member);

        // 更新邀请状态
        invitation.setStatus(TeamInvitation.InvitationStatus.ACCEPTED);
        teamInvitationRepository.save(invitation);

        log.info("邀请已接受: invitationId={}, userId={}", invitationId, userId);
        return team;
    }

    /**
     * 拒绝邀请
     */
    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));

        // 检查被邀请人
        if (invitation.getInviteeId() == null || !invitation.getInviteeId().equals(userId)) {
            throw new BusinessException("您不是被邀请人");
        }

        invitation.setStatus(TeamInvitation.InvitationStatus.REJECTED);
        teamInvitationRepository.save(invitation);

        log.info("邀请已拒绝: invitationId={}, userId={}", invitationId, userId);
    }
}