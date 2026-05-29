package com.lvatong.lft.collaboration;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.CaseCollaboration;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.repository.CaseCollaborationRepository;
import com.lvatong.lft.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseCollaborationService {

    private final CaseCollaborationRepository caseCollaborationRepository;
    private final TeamMemberService teamMemberService;

    /**
     * 共享案件到团队
     */
    @Transactional
    public CaseCollaboration shareCase(Long caseId, Long teamId, Long userId, CaseCollaboration.Permission permission) {
        // 检查是否是团队成员
        TeamMember.MemberRole role = teamMemberService.getMemberRole(teamId, userId);
        if (role == null) {
            throw new BusinessException("您不是该团队成员");
        }

        // 检查是否已共享
        if (caseCollaborationRepository.existsByCaseIdAndTeamId(caseId, teamId)) {
            throw new BusinessException("该案件已共享到此团队");
        }

        CaseCollaboration collaboration = new CaseCollaboration();
        collaboration.setCaseId(caseId);
        collaboration.setTeamId(teamId);
        collaboration.setSharedBy(userId);
        collaboration.setPermission(permission != null ? permission : CaseCollaboration.Permission.VIEW);

        collaboration = caseCollaborationRepository.save(collaboration);
        log.info("案件共享成功: caseId={}, teamId={}", caseId, teamId);
        return collaboration;
    }

    /**
     * 取消共享
     */
    @Transactional
    public void unshareCase(Long caseId, Long teamId, Long userId) {
        // 检查权限
        TeamMember.MemberRole role = teamMemberService.getMemberRole(teamId, userId);
        if (role == null || role == TeamMember.MemberRole.MEMBER) {
            throw new BusinessException("只有管理员或所有者可以取消共享");
        }

        CaseCollaboration collaboration = caseCollaborationRepository.findByCaseIdAndTeamId(caseId, teamId)
                .orElseThrow(() -> new BusinessException("共享记录不存在"));

        caseCollaborationRepository.delete(collaboration);
        log.info("案件取消共享: caseId={}, teamId={}", caseId, teamId);
    }

    /**
     * 获取团队共享的案件列表
     */
    public List<CaseCollaboration> getTeamSharedCases(Long teamId) {
        return caseCollaborationRepository.findByTeamId(teamId);
    }

    /**
     * 获取案件的协作者列表
     */
    public List<CaseCollaboration> getCaseCollaborators(Long caseId) {
        return caseCollaborationRepository.findByCaseId(caseId);
    }

    /**
     * 检查用户是否有案件访问权限
     */
    public boolean hasCaseAccess(Long caseId, Long userId) {
        List<CaseCollaboration> collaborators = caseCollaborationRepository.findByCaseId(caseId);
        for (CaseCollaboration collaboration : collaborators) {
            if (teamMemberService.isTeamMember(collaboration.getTeamId(), userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否有案件编辑权限
     */
    public boolean hasCaseEditAccess(Long caseId, Long userId) {
        List<CaseCollaboration> collaborators = caseCollaborationRepository.findByCaseId(caseId);
        for (CaseCollaboration collaboration : collaborators) {
            TeamMember.MemberRole role = teamMemberService.getMemberRole(collaboration.getTeamId(), userId);
            if (role != null) {
                if (role == TeamMember.MemberRole.OWNER || role == TeamMember.MemberRole.ADMIN) {
                    return true;
                }
                if (collaboration.getPermission() == CaseCollaboration.Permission.EDIT ||
                    collaboration.getPermission() == CaseCollaboration.Permission.ADMIN) {
                    return true;
                }
            }
        }
        return false;
    }
}