package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.recommendation.RecommendationEngine;
import com.lvatong.lft.recommendation.UserBehaviorAnalyzer;
import com.lvatong.lft.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "推荐系统", description = "个性化推荐接口")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationEngine recommendationEngine;
    private final UserBehaviorAnalyzer behaviorAnalyzer;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "获取个性化推荐", description = "基于用户偏好和历史行为的个性化推荐")
    @GetMapping("/personalized")
    public ApiResult<RecommendationEngine.RecommendationResult> getPersonalized(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(request);
        if (userId == null) {
            // 未登录用户返回热门推荐
            return ApiResult.success(recommendationEngine.getPopularRecommendations(limit));
        }
        return ApiResult.success(recommendationEngine.getPersonalizedRecommendations(userId, limit));
    }

    @Operation(summary = "获取基于查询的推荐", description = "根据当前查询推荐相关内容")
    @GetMapping("/query-based")
    public ApiResult<RecommendationEngine.RecommendationResult> getQueryBased(
            HttpServletRequest request,
            @RequestParam String query,
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(request);
        return ApiResult.success(recommendationEngine.getQueryBasedRecommendations(userId, query, domain, limit));
    }

    @Operation(summary = "获取热门推荐", description = "获取全局热门查询推荐")
    @GetMapping("/popular")
    public ApiResult<RecommendationEngine.RecommendationResult> getPopular(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResult.success(recommendationEngine.getPopularRecommendations(limit));
    }

    @Operation(summary = "获取协同过滤推荐", description = "基于相似用户行为的推荐")
    @GetMapping("/collaborative")
    public ApiResult<RecommendationEngine.RecommendationResult> getCollaborative(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ApiResult.success(recommendationEngine.getPopularRecommendations(limit));
        }
        return ApiResult.success(recommendationEngine.getCollaborativeRecommendations(userId, limit));
    }

    @Operation(summary = "记录用户行为", description = "记录用户点击、查看等行为")
    @PostMapping("/behaviors")
    public ApiResult<Void> recordBehavior(
            HttpServletRequest request,
            @RequestParam String actionType,
            @RequestParam String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String domain) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }

        behaviorAnalyzer.recordBehavior(userId,
                com.lvatong.lft.model.entity.recommendation.UserBehavior.ActionType.valueOf(actionType),
                com.lvatong.lft.model.entity.recommendation.UserBehavior.TargetType.valueOf(targetType),
                targetId, query, domain, null);

        return ApiResult.success(null);
    }

    @Operation(summary = "获取用户查询历史", description = "获取用户最近的查询记录")
    @GetMapping("/history")
    public ApiResult<List<String>> getQueryHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }
        return ApiResult.success(behaviorAnalyzer.getUserQueryHistory(userId, limit));
    }

    private Long getUserId(HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.resolveToken(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
