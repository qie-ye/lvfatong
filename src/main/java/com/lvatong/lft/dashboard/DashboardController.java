package com.lvatong.lft.dashboard;

import com.lvatong.lft.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据分析看板", description = "运营数据可视化接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "用户概览：总用户数、日活、周活、月活")
    public ApiResult<Map<String, Object>> getUserOverview() {
        return ApiResult.success(dashboardService.getUserOverview());
    }

    @GetMapping("/query-stats")
    @Operation(summary = "查询统计：日查询量、查询趋势、高峰时段")
    public ApiResult<Map<String, Object>> getQueryStats(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResult.success(dashboardService.getQueryStats(days));
    }

    @GetMapping("/hot-queries")
    @Operation(summary = "热门问题：高频查询词、热门法律领域")
    public ApiResult<Map<String, Object>> getHotQueries(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResult.success(dashboardService.getHotQueries(limit));
    }

    @GetMapping("/ai-performance")
    @Operation(summary = "AI效果：回答准确率、用户满意度")
    public ApiResult<Map<String, Object>> getAIPerformance() {
        return ApiResult.success(dashboardService.getAIPerformance());
    }

    @GetMapping("/recommendation-stats")
    @Operation(summary = "推荐效果：推荐点击率、转化率")
    public ApiResult<Map<String, Object>> getRecommendationStats() {
        return ApiResult.success(dashboardService.getRecommendationStats());
    }

    @GetMapping("/contract-stats")
    @Operation(summary = "合同分析：分析数量、风险分布")
    public ApiResult<Map<String, Object>> getContractStats() {
        return ApiResult.success(dashboardService.getContractStats());
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有Dashboard数据（一次性获取）")
    public ApiResult<Map<String, Object>> getAllDashboardData() {
        return ApiResult.success(dashboardService.getAllDashboardData());
    }
}