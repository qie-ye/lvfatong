package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.Team;
import com.lvatong.lft.model.entity.TeamInvitation;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.team.TeamInvitationService;
import com.lvatong.lft.team.TeamMemberService;
import com.lvatong.lft.team.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "团队创建、成员管理、邀请功能")
public class TeamController {

    private final TeamService teamService;
    private final TeamMemberService teamMemberService;
    private final TeamInvitationService teamInvitationService;

    // ==================== 团队操作 ====================

    @PostMapping
    @Operation(summary = "创建团队")
    public ApiResult<Team> createTeam(@RequestBody CreateTeamRequest request) {
        // TODO: 从SecurityContext获取当前用户ID
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamService.createTeam(userId, request.getName(), request.getDescription()));
    }

    @GetMapping
    @Operation(summary = "获取我的团队列表")
    public ApiResult<List<Team>> getMyTeams() {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamService.getUserTeams(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取团队详情")
    public ApiResult<Team> getTeam(@PathVariable Long id) {
        return ApiResult.success(teamService.getTeam(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新团队信息")
    public ApiResult<Team> updateTeam(@PathVariable Long id, @RequestBody UpdateTeamRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamService.updateTeam(id, userId, request.getName(), request.getDescription()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "解散团队")
    public ApiResult<Void> disbandTeam(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        teamService.disbandTeam(id, userId);
        return ApiResult.success(null);
    }

    @PostMapping("/{id}/refresh-invite-code")
    @Operation(summary = "刷新邀请码")
    public ApiResult<Map<String, String>> refreshInviteCode(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        String inviteCode = teamService.refreshInviteCode(id, userId);
        return ApiResult.success(Map.of("inviteCode", inviteCode));
    }

    @PostMapping("/join/{inviteCode}")
    @Operation(summary = "通过邀请码加入团队")
    public ApiResult<Team> joinByInviteCode(@PathVariable String inviteCode) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamService.joinByInviteCode(userId, inviteCode));
    }

    // ==================== 成员操作 ====================

    @GetMapping("/{id}/members")
    @Operation(summary = "获取团队成员列表")
    public ApiResult<List<TeamMemberService.MemberDTO>> getTeamMembers(@PathVariable Long id) {
        return ApiResult.success(teamMemberService.getTeamMemberDetails(id));
    }

    @PutMapping("/{id}/members/{userId}/role")
    @Operation(summary = "修改成员角色")
    public ApiResult<Void> updateMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                             @RequestBody UpdateRoleRequest request) {
        Long operatorId = 1L; // 临时硬编码
        teamMemberService.updateMemberRole(id, userId, operatorId, request.getRole());
        return ApiResult.success(null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "移除成员")
    public ApiResult<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        Long operatorId = 1L; // 临时硬编码
        teamMemberService.removeMember(id, userId, operatorId);
        return ApiResult.success(null);
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "退出团队")
    public ApiResult<Void> leaveTeam(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        teamMemberService.leaveTeam(id, userId);
        return ApiResult.success(null);
    }

    // ==================== 邀请操作 ====================

    @PostMapping("/{id}/invite")
    @Operation(summary = "邀请成员")
    public ApiResult<TeamInvitation> inviteMember(@PathVariable Long id, @RequestBody InviteRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamInvitationService.inviteByPhone(id, userId, request.getPhone()));
    }

    @GetMapping("/{id}/invitations")
    @Operation(summary = "获取团队邀请列表")
    public ApiResult<List<TeamInvitation>> getTeamInvitations(@PathVariable Long id) {
        return ApiResult.success(teamInvitationService.getTeamInvitations(id));
    }

    @GetMapping("/invitations/my")
    @Operation(summary = "获取我收到的邀请")
    public ApiResult<List<TeamInvitation>> getMyInvitations() {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamInvitationService.getUserInvitations(userId));
    }

    @PostMapping("/invitations/{id}/accept")
    @Operation(summary = "接受邀请")
    public ApiResult<Team> acceptInvitation(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamInvitationService.acceptInvitation(id, userId));
    }

    @PostMapping("/invitations/{id}/reject")
    @Operation(summary = "拒绝邀请")
    public ApiResult<Void> rejectInvitation(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        teamInvitationService.rejectInvitation(id, userId);
        return ApiResult.success(null);
    }

    // ==================== 请求类 ====================

    @Data
    public static class CreateTeamRequest {
        private String name;
        private String description;
    }

    @Data
    public static class UpdateTeamRequest {
        private String name;
        private String description;
    }

    @Data
    public static class UpdateRoleRequest {
        private TeamMember.MemberRole role;
    }

    @Data
    public static class InviteRequest {
        private String phone;
    }
}