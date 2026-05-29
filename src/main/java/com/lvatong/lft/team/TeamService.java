package com.lvatong.lft.team;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Team;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.repository.TeamMemberRepository;
import com.lvatong.lft.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 创建团队
     */
    @Transactional
    public Team createTeam(Long userId, String name, String description) {
        // 检查是否已存在同名团队
        if (teamRepository.existsByOwnerIdAndName(userId, name)) {
            throw new BusinessException("您已创建过同名团队");
        }

        // 创建团队
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setOwnerId(userId);
        team.setInviteCode(generateInviteCode());
        team.setStatus(Team.TeamStatus.ACTIVE);
        team = teamRepository.save(team);

        // 将创建者添加为所有者
        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeamId(team.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(TeamMember.MemberRole.OWNER);
        teamMemberRepository.save(ownerMember);

        log.info("团队创建成功: teamId={}, ownerId={}", team.getId(), userId);
        return team;
    }

    /**
     * 获取团队详情
     */
    public Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("团队不存在"));
    }

    /**
     * 获取用户的团队列表
     */
    public List<Team> getUserTeams(Long userId) {
        return teamRepository.findTeamsByUserId(userId);
    }

    /**
     * 获取用户创建的团队列表
     */
    public List<Team> getOwnedTeams(Long userId) {
        return teamRepository.findByOwnerIdAndStatus(userId, Team.TeamStatus.ACTIVE);
    }

    /**
     * 更新团队信息
     */
    @Transactional
    public Team updateTeam(Long teamId, Long userId, String name, String description) {
        Team team = getTeam(teamId);

        // 检查权限
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException("只有团队创建者可以修改团队信息");
        }

        if (name != null && !name.isEmpty()) {
            team.setName(name);
        }
        if (description != null) {
            team.setDescription(description);
        }

        team = teamRepository.save(team);
        log.info("团队信息更新: teamId={}", teamId);
        return team;
    }

    /**
     * 解散团队
     */
    @Transactional
    public void disbandTeam(Long teamId, Long userId) {
        Team team = getTeam(teamId);

        // 检查权限
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException("只有团队创建者可以解散团队");
        }

        team.setStatus(Team.TeamStatus.DISBANDED);
        teamRepository.save(team);

        log.info("团队已解散: teamId={}", teamId);
    }

    /**
     * 刷新邀请码
     */
    @Transactional
    public String refreshInviteCode(Long teamId, Long userId) {
        Team team = getTeam(teamId);

        // 检查权限
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException("只有团队创建者可以刷新邀请码");
        }

        team.setInviteCode(generateInviteCode());
        teamRepository.save(team);

        log.info("邀请码已刷新: teamId={}", teamId);
        return team.getInviteCode();
    }

    /**
     * 通过邀请码加入团队
     */
    @Transactional
    public Team joinByInviteCode(Long userId, String inviteCode) {
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException("邀请码无效"));

        if (team.getStatus() != Team.TeamStatus.ACTIVE) {
            throw new BusinessException("团队已解散");
        }

        // 检查是否已是成员
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
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

        log.info("用户加入团队: userId={}, teamId={}", userId, team.getId());
        return team;
    }

    /**
     * 生成邀请码
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}