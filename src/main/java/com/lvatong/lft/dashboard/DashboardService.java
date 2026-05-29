package com.lvatong.lft.dashboard;

import com.lvatong.lft.model.entity.AnswerFeedback;
import com.lvatong.lft.repository.*;
import com.lvatong.lft.repository.recommendation.UserBehaviorRepository;
import com.lvatong.lft.repository.recommendation.RecommendationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final ContractDocumentRepository contractDocumentRepository;

    /**
     * 用户概览：总用户数、日活、周活、月活
     */
    public Map<String, Object> getUserOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userRepository.count());
        result.put("dailyActiveUsers", chatSessionRepository.countActiveUsersSince(LocalDate.now().atStartOfDay()));
        result.put("weeklyActiveUsers", chatSessionRepository.countActiveUsersSince(LocalDate.now().minusWeeks(1).atStartOfDay()));
        result.put("monthlyActiveUsers", chatSessionRepository.countActiveUsersSince(LocalDate.now().minusMonths(1).atStartOfDay()));
        return result;
    }

    /**
     * 查询统计：日查询量、查询趋势、高峰时段
     */
    public Map<String, Object> getQueryStats(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        
        // 日查询量趋势
        List<Object[]> dailyQueryCounts = chatMessageRepository.countByDaySince(since);
        List<Map<String, Object>> trend = dailyQueryCounts.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("dailyTrend", trend);
        
        // 高峰时段（按小时统计）
        List<Object[]> hourlyCounts = chatMessageRepository.countByHourSince(since);
        List<Map<String, Object>> peakHours = hourlyCounts.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("hour", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("peakHours", peakHours);
        
        // 总查询量
        long totalQueries = chatMessageRepository.countByCreatedAtAfter(since);
        result.put("totalQueries", totalQueries);
        
        return result;
    }

    /**
     * 热门问题：高频查询词、热门法律领域
     */
    public Map<String, Object> getHotQueries(int limit) {
        Map<String, Object> result = new HashMap<>();
        
        // 高频查询词（从user_behaviors表统计）
        List<Object[]> hotKeywords = userBehaviorRepository.findHotKeywords(limit);
        List<Map<String, Object>> keywords = hotKeywords.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("keyword", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("hotKeywords", keywords);
        
        // 热门法律领域
        List<Object[]> hotDomains = userBehaviorRepository.findHotDomains(limit);
        List<Map<String, Object>> domains = hotDomains.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("domain", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("hotDomains", domains);
        
        return result;
    }

    /**
     * AI效果：回答准确率、用户满意度
     */
    public Map<String, Object> getAIPerformance() {
        Map<String, Object> result = new HashMap<>();
        
        // 用户满意度（好评率）
        long goodCount = answerFeedbackRepository.countByRating(AnswerFeedback.Rating.GOOD);
        long badCount = answerFeedbackRepository.countByRating(AnswerFeedback.Rating.BAD);
        long totalFeedback = goodCount + badCount;
        double satisfactionRate = totalFeedback == 0 ? 0.0 : Math.round((double) goodCount / totalFeedback * 1000) / 10.0;
        result.put("satisfactionRate", satisfactionRate);
        result.put("totalFeedback", totalFeedback);
        result.put("goodCount", goodCount);
        result.put("badCount", badCount);
        
        // 回答准确率（基于反馈数据估算）
        // 这里简化处理，实际可以基于更复杂的逻辑
        result.put("estimatedAccuracy", satisfactionRate);
        
        return result;
    }

    /**
     * 推荐效果：推荐点击率、转化率
     */
    public Map<String, Object> getRecommendationStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 推荐点击率
        long totalRecommendations = recommendationLogRepository.count();
        long clickedRecommendations = recommendationLogRepository.countByClicked(true);
        double clickRate = totalRecommendations == 0 ? 0.0 : Math.round((double) clickedRecommendations / totalRecommendations * 1000) / 10.0;
        result.put("totalRecommendations", totalRecommendations);
        result.put("clickedRecommendations", clickedRecommendations);
        result.put("clickRate", clickRate);
        
        // 转化率（有反馈的推荐比例）
        long feedbackRecommendations = recommendationLogRepository.countByFeedbackRatingIsNotNull();
        double conversionRate = totalRecommendations == 0 ? 0.0 : Math.round((double) feedbackRecommendations / totalRecommendations * 1000) / 10.0;
        result.put("conversionRate", conversionRate);
        
        return result;
    }

    /**
     * 合同分析：分析数量、风险分布
     */
    public Map<String, Object> getContractStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 合同分析总量
        long totalContracts = contractDocumentRepository.count();
        result.put("totalContracts", totalContracts);
        
        // 风险分布（按状态统计）
        List<Object[]> statusDistribution = contractDocumentRepository.countByStatusGroup();
        List<Map<String, Object>> riskDistribution = statusDistribution.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("status", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("riskDistribution", riskDistribution);
        
        // 最近30天合同分析趋势
        List<Object[]> dailyContractCounts = contractDocumentRepository.countByDaySince(LocalDate.now().minusDays(30).atStartOfDay());
        List<Map<String, Object>> trend = dailyContractCounts.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", row[0].toString());
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("dailyTrend", trend);
        
        return result;
    }

    /**
     * 获取所有Dashboard数据（一次性获取）
     */
    public Map<String, Object> getAllDashboardData() {
        Map<String, Object> result = new HashMap<>();
        result.put("userOverview", getUserOverview());
        result.put("queryStats", getQueryStats(30));
        result.put("hotQueries", getHotQueries(10));
        result.put("aiPerformance", getAIPerformance());
        result.put("recommendationStats", getRecommendationStats());
        result.put("contractStats", getContractStats());
        return result;
    }
}