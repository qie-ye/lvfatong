package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.FeedbackRequest;
import com.lvatong.lft.model.dto.FeedbackStatsResponse;
import com.lvatong.lft.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "AI质量反馈", description = "用户对AI回答的点赞/踩及统计")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "提交反馈（每条消息每用户一次）",
               description = "支持点踩原因和问题分类标签，用于后续分析和优化")
    public ApiResult<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        feedbackService.submitFeedback(userId, request);
        return ApiResult.success(null);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取全局好评率统计")
    public ApiResult<FeedbackStatsResponse> getStats() {
        return ApiResult.success(feedbackService.getGlobalStats());
    }
}
