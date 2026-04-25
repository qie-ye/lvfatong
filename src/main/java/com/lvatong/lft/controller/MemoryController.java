package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.UserMemory;
import com.lvatong.lft.service.UserMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/memories")
@RequiredArgsConstructor
@Tag(name = "用户记忆", description = "查看和管理AI记住的用户偏好和历史")
public class MemoryController {

    private final UserMemoryService userMemoryService;

    @GetMapping
    @Operation(summary = "查看用户记忆列表")
    public ApiResult<List<UserMemory>> getMemories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(userMemoryService.getUserMemories(userId));
    }

    @DeleteMapping("/{memoryId}")
    @Operation(summary = "删除单条记忆")
    public ApiResult<String> deleteMemory(@PathVariable("memoryId") Long memoryId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean deleted = userMemoryService.deleteUserMemory(userId, memoryId);
        return deleted ? ApiResult.success("记忆已删除") : ApiResult.error("记忆不存在或无权限");
    }
}
