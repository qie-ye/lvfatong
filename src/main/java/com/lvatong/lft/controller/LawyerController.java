package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.*;
import com.lvatong.lft.service.LawyerReviewService;
import com.lvatong.lft.service.LawyerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/lawyers")
@RequiredArgsConstructor
@Tag(name = "律师服务", description = "律师档案、搜索推荐、预约管理")
public class LawyerController {

    private final LawyerService lawyerService;
    private final LawyerReviewService lawyerReviewService;

    // ========== 律师档案 ==========

    @PostMapping("/profile")
    @Operation(summary = "创建/更新律师档案")
    public ApiResult<LawyerProfileResponse> createOrUpdateProfile(
            @Valid @RequestBody CreateLawyerProfileRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.createOrUpdateProfile(userId, request));
    }

    @GetMapping("/profile/me")
    @Operation(summary = "获取我的律师档案")
    public ApiResult<LawyerProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.getMyProfile(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取律师详情")
    public ApiResult<LawyerProfileResponse> getProfile(@PathVariable("id") Long id) {
        return ApiResult.success(lawyerService.getProfile(id));
    }

    // ========== 律师搜索 ==========

    @GetMapping
    @Operation(summary = "律师列表（按评分排序）")
    public ApiResult<Page<LawyerProfileResponse>> listLawyers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResult.success(lawyerService.listLawyers(page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索律师")
    public ApiResult<Page<LawyerProfileResponse>> search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "specialty", required = false) String specialty,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        if (specialty != null && !specialty.isBlank()) {
            return ApiResult.success(lawyerService.searchBySpecialty(specialty, page, size));
        }
        if (keyword != null && !keyword.isBlank()) {
            return ApiResult.success(lawyerService.searchByKeyword(keyword, page, size));
        }
        return ApiResult.success(lawyerService.listLawyers(page, size));
    }

    @PostMapping("/recommend")
    @RateLimit(permitsPerSecond = 5.0, dimension = "USER", message = "律师推荐请求过于频繁，请稍后再试")
    @Operation(summary = "AI推荐律师")
    public ApiResult<List<LawyerProfileResponse>> recommend(
            @RequestParam("question") String question,
            @RequestParam(name = "topK", defaultValue = "5") int topK) {
        return ApiResult.success(lawyerService.recommendLawyers(question, topK));
    }

    // ========== 预约管理 ==========

    @PostMapping("/appointments")
    @Operation(summary = "创建预约")
    public ApiResult<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.createAppointment(userId, request));
    }

    @GetMapping("/appointments")
    @Operation(summary = "我的预约列表")
    public ApiResult<List<AppointmentResponse>> getMyAppointments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.getUserAppointments(userId));
    }

    @PutMapping("/appointments/{id}/confirm")
    @Operation(summary = "确认预约（律师端）")
    public ApiResult<AppointmentResponse> confirmAppointment(
            @PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.confirmAppointment(userId, id));
    }

    @PutMapping("/appointments/{id}/cancel")
    @Operation(summary = "取消预约")
    public ApiResult<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false) String reason,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerService.cancelAppointment(userId, id, reason));
    }

    // ========== 律师评价 ==========

    @PostMapping("/{id}/reviews")
    @Operation(summary = "评价律师")
    public ApiResult<LawyerReviewResponse> createReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        request.setLawyerId(id);
        return ApiResult.success(lawyerReviewService.createReview(userId, request));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "获取律师评价列表")
    public ApiResult<List<LawyerReviewResponse>> getLawyerReviews(@PathVariable("id") Long id) {
        return ApiResult.success(lawyerReviewService.getLawyerReviews(id));
    }

    @GetMapping("/reviews/mine")
    @Operation(summary = "我的评价列表")
    public ApiResult<List<LawyerReviewResponse>> getMyReviews(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(lawyerReviewService.getUserReviews(userId));
    }

    @GetMapping("/recommend/cf")
    @RateLimit(permitsPerSecond = 3.0, dimension = "USER", message = "推荐请求过于频繁，请稍后再试")
    @Operation(summary = "协同过滤推荐律师")
    public ApiResult<List<LawyerProfileResponse>> collaborativeRecommend(
            @RequestParam(name = "topK", defaultValue = "5") int topK,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Long> lawyerIds = lawyerReviewService.getCollaborativeRecommendations(userId, topK);
        List<LawyerProfileResponse> result = lawyerIds.stream()
                .map(id -> lawyerService.getProfile(id))
                .toList();
        return ApiResult.success(result);
    }
}
