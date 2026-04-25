package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.GenerateOpinionRequest;
import com.lvatong.lft.model.dto.LegalOpinionResponse;
import com.lvatong.lft.service.LegalOpinionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/opinions")
@RequiredArgsConstructor
@Tag(name = "法律意见", description = "AI深度推理生成法律意见书")
public class LegalOpinionController {

    private final LegalOpinionService legalOpinionService;

    @PostMapping
    @RateLimit(permitsPerSecond = 1.0, dimension = "USER", message = "法律意见生成请求过于频繁，请稍后再试")
    @Operation(summary = "生成法律意见书（异步，使用GLM-4-Plus深度推理）")
    public ApiResult<LegalOpinionResponse> generate(
            @Valid @RequestBody GenerateOpinionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalOpinionService.generateOpinion(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取法律意见详情")
    public ApiResult<LegalOpinionResponse> getOpinion(
            @PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalOpinionService.getOpinion(userId, id));
    }

    @GetMapping
    @Operation(summary = "获取用户的法律意见列表")
    public ApiResult<List<LegalOpinionResponse>> list(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalOpinionService.listByUser(userId));
    }
}
