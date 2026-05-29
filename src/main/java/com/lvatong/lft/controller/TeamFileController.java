package com.lvatong.lft.controller;

import com.lvatong.lft.collaboration.TeamFileService;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.TeamFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/files")
@RequiredArgsConstructor
@Tag(name = "团队文件", description = "团队文件上传、下载、管理")
public class TeamFileController {

    private final TeamFileService teamFileService;

    @PostMapping
    @Operation(summary = "上传文件")
    public ApiResult<TeamFile> uploadFile(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long caseId) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(teamFileService.uploadFile(teamId, userId, file, caseId));
    }

    @GetMapping
    @Operation(summary = "获取团队文件列表")
    public ApiResult<List<TeamFile>> getTeamFiles(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long caseId) {
        return ApiResult.success(teamFileService.getTeamFiles(teamId, caseId));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "获取文件详情")
    public ApiResult<TeamFile> getFile(@PathVariable Long teamId, @PathVariable Long fileId) {
        return ApiResult.success(teamFileService.getFile(fileId));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ApiResult<Void> deleteFile(@PathVariable Long teamId, @PathVariable Long fileId) {
        Long userId = 1L; // 临时硬编码
        teamFileService.deleteFile(fileId, userId);
        return ApiResult.success(null);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取文件统计")
    public ApiResult<TeamFileService.FileStats> getFileStats(@PathVariable Long teamId) {
        return ApiResult.success(teamFileService.getTeamFileStats(teamId));
    }
}