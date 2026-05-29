package com.lvatong.lft.recommendation;

import com.lvatong.lft.model.entity.FaqEntry;
import com.lvatong.lft.model.entity.LegalCase;
import com.lvatong.lft.model.entity.recommendation.PopularQuery;
import com.lvatong.lft.model.entity.recommendation.RecommendationLog;
import com.lvatong.lft.model.entity.recommendation.UserBehavior;
import com.lvatong.lft.repository.recommendation.PopularQueryRepository;
import com.lvatong.lft.repository.recommendation.RecommendationLogRepository;
import com.lvatong.lft.repository.FaqEntryRepository;
import com.lvatong.lft.repository.LegalCaseRepository;
import com.lvatong.lft.rag.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐引擎
 *
 * 推荐策略：
 * 1. 个性化推荐：基于用户偏好和历史行为
 * 2. 热门推荐：基于全局热门查询
 * 3. 相似内容推荐：基于语义相似度
 * 4. 协同过滤：基于相似用户的行为
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final UserBehaviorAnalyzer behaviorAnalyzer;
    private final PopularQueryRepository popularQueryRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final HybridSearchService hybridSearchService;
    private final FaqEntryRepository faqEntryRepository;
    private final LegalCaseRepository legalCaseRepository;

    /**
     * 获取个性化推荐
     */
    public RecommendationResult getPersonalizedRecommendations(Long userId, int limit) {
        List<RecommendationItem> items = new ArrayList<>();

        // 1. 基于用户偏好领域推荐相关内容
        List<String> preferredDomains = behaviorAnalyzer.getPreferredDomains(userId);
        if (!preferredDomains.isEmpty()) {
            items.addAll(recommendByDomains(preferredDomains, limit / 2));
        }

        // 2. 补充热门内容
        if (items.size() < limit) {
            items.addAll(getPopularItems(limit - items.size()));
        }

        // 3. 去重并限制数量
        items = deduplicateAndLimit(items, limit);

        // 记录推荐日志
        saveRecommendationLog(userId, RecommendationLog.RecommendationType.TOPIC, items);

        return RecommendationResult.builder()
                .items(items)
                .strategy("personalized")
                .build();
    }

    /**
     * 获取基于当前查询的推荐
     */
    public RecommendationResult getQueryBasedRecommendations(Long userId, String query, String domain, int limit) {
        List<RecommendationItem> items = new ArrayList<>();

        // 1. 推荐相关FAQ
        List<FaqEntry> relatedFaqs = findRelatedFaqs(query, limit / 3);
        for (FaqEntry faq : relatedFaqs) {
            items.add(RecommendationItem.builder()
                    .id(faq.getId())
                    .type(RecommendationItem.ItemType.FAQ)
                    .title(faq.getQuestion())
                    .description(truncate(faq.getAnswer(), 100))
                    .score(0.8)
                    .build());
        }

        // 2. 推荐相关案例
        List<HybridSearchService.SearchResult> relatedCases = hybridSearchService.search(query, "CASE", domain, limit / 3);
        for (HybridSearchService.SearchResult result : relatedCases) {
            items.add(RecommendationItem.builder()
                    .type(RecommendationItem.ItemType.CASE)
                    .title(truncate(result.content(), 50))
                    .description(truncate(result.content(), 150))
                    .score(result.score())
                    .build());
        }

        // 3. 推荐相关法条
        List<HybridSearchService.SearchResult> relatedLaws = hybridSearchService.search(query, "LAW", domain, limit / 3);
        for (HybridSearchService.SearchResult result : relatedLaws) {
            items.add(RecommendationItem.builder()
                    .type(RecommendationItem.ItemType.LAW)
                    .title(truncate(result.content(), 50))
                    .description(truncate(result.content(), 150))
                    .score(result.score())
                    .build());
        }

        items = deduplicateAndLimit(items, limit);

        // 记录搜索行为
        if (userId != null) {
            behaviorAnalyzer.recordSearch(userId, query, domain);
        }

        return RecommendationResult.builder()
                .items(items)
                .strategy("query_based")
                .build();
    }

    /**
     * 获取热门推荐
     */
    public RecommendationResult getPopularRecommendations(int limit) {
        List<PopularQuery> popularQueries = popularQueryRepository.findTopQueries(PageRequest.of(0, limit));

        List<RecommendationItem> items = popularQueries.stream()
                .map(pq -> RecommendationItem.builder()
                        .type(RecommendationItem.ItemType.TOPIC)
                        .title(pq.getQueryText())
                        .description("热门查询 (" + pq.getQueryCount() + "次)")
                        .score(1.0)
                        .build())
                .collect(Collectors.toList());

        return RecommendationResult.builder()
                .items(items)
                .strategy("popular")
                .build();
    }

    /**
     * 获取热门推荐项目列表
     */
    private List<RecommendationItem> getPopularItems(int limit) {
        List<PopularQuery> popularQueries = popularQueryRepository.findTopQueries(PageRequest.of(0, limit));
        return popularQueries.stream()
                .map(pq -> RecommendationItem.builder()
                        .type(RecommendationItem.ItemType.TOPIC)
                        .title(pq.getQueryText())
                        .description("热门查询 (" + pq.getQueryCount() + "次)")
                        .score(1.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取相似用户查询推荐（简单协同过滤）
     */
    public RecommendationResult getCollaborativeRecommendations(Long userId, int limit) {
        // 获取用户最近查询
        List<String> recentQueries = behaviorAnalyzer.getUserQueryHistory(userId, 5);
        if (recentQueries.isEmpty()) {
            return getPopularRecommendations(limit);
        }

        // 基于最近查询进行推荐
        String lastQuery = recentQueries.get(0);
        return getQueryBasedRecommendations(userId, lastQuery, null, limit);
    }

    /**
     * 基于领域推荐
     */
    private List<RecommendationItem> recommendByDomains(List<String> domains, int limit) {
        List<RecommendationItem> items = new ArrayList<>();

        for (String domain : domains) {
            // 搜索该领域的FAQ
            List<FaqEntry> faqs = faqEntryRepository.findByCategory(domain, PageRequest.of(0, 2));
            for (FaqEntry faq : faqs) {
                items.add(RecommendationItem.builder()
                        .id(faq.getId())
                        .type(RecommendationItem.ItemType.FAQ)
                        .title(faq.getQuestion())
                        .description(truncate(faq.getAnswer(), 100))
                        .score(0.7)
                        .build());
            }

            if (items.size() >= limit) break;
        }

        return items;
    }

    /**
     * 查找相关FAQ
     */
    private List<FaqEntry> findRelatedFaqs(String query, int limit) {
        // 简单关键词匹配
        List<FaqEntry> results = faqEntryRepository.searchByKeyword(query, PageRequest.of(0, limit));
        return results;
    }

    /**
     * 去重并限制数量
     */
    private List<RecommendationItem> deduplicateAndLimit(List<RecommendationItem> items, int limit) {
        Set<String> seen = new HashSet<>();
        return items.stream()
                .filter(item -> {
                    String key = item.getType() + ":" + item.getTitle();
                    if (seen.contains(key)) return false;
                    seen.add(key);
                    return true;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 保存推荐日志
     */
    private void saveRecommendationLog(Long userId, RecommendationLog.RecommendationType type,
                                        List<RecommendationItem> items) {
        if (userId == null) return;

        try {
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setRecommendationType(type);
            log.setRecommendedItems(items.toString());
            log.setAlgorithmVersion("v1");
            recommendationLogRepository.save(log);
        } catch (Exception e) {
            // 记录日志失败不影响主流程
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ==================== 数据模型 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationResult {
        private List<RecommendationItem> items;
        private String strategy;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationItem {
        private Long id;
        private ItemType type;
        private String title;
        private String description;
        private double score;
        private String url;

        public enum ItemType {
            LAW, CASE, FAQ, LAWYER, TOPIC, DOCUMENT
        }
    }
}
