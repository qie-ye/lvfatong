package com.lvatong.lft.team;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Team;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.TeamMemberRepository;
import com.lvatong.lft.repository.TeamRepository;
import com.lvatong.lft.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * 获取团队成员列表
     */
    public List<TeamMember> getTeamMembers(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    /**
     * 获取团队成员详情（包含用户信息）
     */
    public List<MemberDTO> getTeamMemberDetails(Long teamId) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        return members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    MemberDTO dto = new MemberDTO();
                    dto.setMemberId(member.getId());
                    dto.setUserId(member.getUserId());
                    dto.setRole(member.getRole());
                    dto.setJoinedAt(member.getJoinedAt());
                    if (user != null) {
                        dto.setUsername(user.getUsername());
                        dto.setNickname(user.getNickname());
                        dto.setEmail(user.getEmail());
                        dto.setPhone(user.getPhone());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是团队成员
     */
    public boolean isTeamMember(Long teamId, Long userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    /**
     * 获取用户在团队中的角色
     */
    public TeamMember.MemberRole getMemberRole(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getRole)
                .orElse(null);
    }

    /**
     * 修改成员角色
     */
    @Transactional
    public void updateMemberRole(Long teamId, Long targetUserId, Long operatorUserId, TeamMember.MemberRole newRole) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("团队不存在"));

        // 检查操作者权限
        TeamMember.MemberRole operatorRole = getMemberRole(teamId, operatorUserId);
        if (operatorRole != TeamMember.MemberRole.OWNER && operatorRole != TeamMember.MemberRole.ADMIN) {
            throw new BusinessException("只有所有者或管理员可以修改成员角色");
        }

        // 不能修改自己的角色
        if (targetUserId.equals(operatorUserId)) {
            throw new BusinessException("不能修改自己的角色");
        }

        // 获取目标成员
        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new BusinessException("该用户不是团队成员"));

        // 管理员不能提升其他成员为管理员或所有者
        if (operatorRole == TeamMember.MemberRole.ADMIN && 
            (newRole == TeamMember.MemberRole.OWNER || newRole == TeamMember.MemberRole.ADMIN)) {
            throw new BusinessException("管理员不能提升其他成员为管理员或所有者");
        }

        targetMember.setRole(newRole);
        teamMemberRepository.save(targetMember);

        log.info("成员角色已更新: teamId={}, userId={}, newRole={}", teamId, targetUserId, newRole);
    }

    /**
     * 移除成员
     */
    @Transactional
    public void removeMember(Long teamId, Long targetUserId, Long operatorUserId) {
        // 检查操作者权限
        TeamMember.MemberRole operatorRole = getMemberRole(teamId, operatorUserId);
        if (operatorRole != TeamMember.MemberRole.OWNER && operatorRole != TeamMember.MemberRole.ADMIN) {
            throw new BusinessException("只有所有者或管理员可以移除成员");
        }

        // 不能移除自己
        if (targetUserId.equals(operatorUserId)) {
            throw new BusinessException("不能移除自己，请使用退出团队功能");
        }

        // 获取目标成员
        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new BusinessException("该用户不是团队成员"));

        // 管理员不能移除其他管理员或所有者
        if (operatorRole == TeamMember.MemberRole.ADMIN && 
            (targetMember.getRole() == TeamMember.MemberRole.OWNER || targetMember.getRole() == TeamMember.MemberRole.ADMIN)) {
            throw new BusinessException("管理员不能移除其他管理员或所有者");
        }

        teamMemberRepository.delete(targetMember);

        log.info("成员已移除: teamId={}, userId={}", teamId, targetUserId);
    }

    /**
     * 退出团队
     */
    @Transactional
    public void leaveTeam(Long teamId, Long userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException("您不是该团队成员"));

        // 所有者不能退出团队
        if (member.getRole() == TeamMember.MemberRole.OWNER) {
            throw new BusinessException("团队所有者不能退出，请先转让团队或解散团队");
        }

        teamMemberRepository.delete(member);

        log.info("用户退出团队: userId={}, teamId={}", userId, teamId);
    }

    /**
     * 成员DTO
     */
    @Data
    public static class MemberDTO {
        private Long memberId;
        private Long userId;
        private TeamMember.MemberRole role;
        private java.time.LocalDateTime joinedAt;
        private String username;
        private String nickname;
        private String email;
        private String phone;
    }
}