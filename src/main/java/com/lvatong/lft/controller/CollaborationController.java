package com.lvatong.lft.controller;

import com.lvatong.lft.collaboration.ActivityLogService;
import com.lvatong.lft.collaboration.CaseCollaborationService;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.CaseActivityLog;
import com.lvatong.lft.model.entity.CaseCollaboration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "案件协作", description = "案件共享、协作管理、活动日志")
public class CollaborationController {

    private final CaseCollaborationService caseCollaborationService;
    private final ActivityLogService activityLogService;

    @PostMapping("/{id}/share")
    @Operation(summary = "共享案件到团队")
    public ApiResult<CaseCollaboration> shareCase(@PathVariable Long id, @RequestBody ShareCaseRequest request) {
        Long userId = 1L; // 临时硬编码
        CaseCollaboration collaboration = caseCollaborationService.shareCase(id, request.getTeamId(), 
                userId, request.getPermission());
        
        // 记录日志
        activityLogService.logActivity(id, userId, "CASE_SHARED", "案件共享到团队: " + request.getTeamId());
        
        return ApiResult.success(collaboration);
    }

    @DeleteMapping("/{id}/share/{teamId}")
    @Operation(summary = "取消共享")
    public ApiResult<Void> unshareCase(@PathVariable Long id, @PathVariable Long teamId) {
        Long userId = 1L; // 临时硬编码
        caseCollaborationService.unshareCase(id, teamId, userId);
        
        // 记录日志
        activityLogService.logActivity(id, userId, "CASE_UNSHARED", "案件取消共享: " + teamId);
        
        return ApiResult.success(null);
    }

    @GetMapping("/{id}/collaborators")
    @Operation(summary = "获取协作者列表")
    public ApiResult<List<CaseCollaboration>> getCaseCollaborators(@PathVariable Long id) {
        return ApiResult.success(caseCollaborationService.getCaseCollaborators(id));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "获取案件日志")
    public ApiResult<List<CaseActivityLog>> getCaseLogs(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "false") boolean recent) {
        if (recent) {
            return ApiResult.success(activityLogService.getRecentActivityLogs(id));
        }
        return ApiResult.success(activityLogService.getCaseActivityLogs(id));
    }

    // ==================== 请求类 ====================

    @Data
    public static class ShareCaseRequest {
        private Long teamId;
        private CaseCollaboration.Permission permission;
    }
}