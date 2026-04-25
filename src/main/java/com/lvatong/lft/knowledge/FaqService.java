package com.lvatong.lft.knowledge;

import com.lvatong.lft.model.entity.FaqEntry;
import com.lvatong.lft.repository.FaqEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqEntryRepository faqEntryRepository;

    private static final double MIN_FULLTEXT_SCORE = 5.0;
    private static final double MIN_KEYWORD_OVERLAP = 0.2;

    /**
     * 获取所有启用的FAQ
     */
    @Cacheable(value = "faqCache", key = "'all'")
    public List<FaqEntry> listAll() {
        return faqEntryRepository.findByEnabledTrueOrderByIdAsc();
    }

    /**
     * 按分类查询
     */
    @Cacheable(value = "faqCache", key = "'cat:' + #category")
    public List<FaqEntry> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return listAll();
        }
        return faqEntryRepository.findByCategoryAndEnabledTrue(category);
    }

    /**
     * 全文搜索FAQ
     */
    public List<FaqEntry> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return faqEntryRepository.fulltextSearch(query, limit);
        } catch (Exception e) {
            log.warn("FAQ fulltext search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 优先匹配FAQ（用于法律问答前置判断）
     * 返回null表示无匹配，返回FaqEntry表示命中高频问题
     * 使用关键词重叠度+全文搜索分数双重验证，避免误匹配
     */
    public FaqEntry matchFaq(String question) {
        if (question == null || question.isBlank()) return null;
        try {
            // Step 1: 全文搜索 top1 with score
            var result = faqEntryRepository.fulltextSearchTopWithScore(question);
            if (result.isEmpty()) return null;

            Object[] row = result.get();
            // row[0..N-1] = FaqEntry字段, row[N] = score
            // score在最后一位
            double score = ((Number) row[row.length - 1]).doubleValue();
            if (score < MIN_FULLTEXT_SCORE) {
                log.debug("FAQ score too low ({}) for: {}", score, question);
                return null;
            }

            // Step 2: 从row中重建FaqEntry（id在第一位）
            Long faqId = ((Number) row[0]).longValue();
            FaqEntry match = faqEntryRepository.findById(faqId).orElse(null);
            if (match == null || !match.getEnabled()) return null;

            // Step 3: 关键词重叠度验证
            double overlap = keywordOverlap(question, match.getQuestion());
            if (overlap < MIN_KEYWORD_OVERLAP) {
                log.debug("FAQ keyword overlap too low ({}) for: {} vs {}", overlap, question, match.getQuestion());
                return null;
            }

            log.info("FAQ matched (score={}, overlap={}): {} -> {}", score, overlap, question, match.getQuestion());
            return match;
        } catch (Exception e) {
            log.debug("FAQ match failed: {}", e.getMessage());
        }
        return null;
    }

    private double keywordOverlap(String question, String faqQuestion) {
        Set<String> qWords = extractKeywords(question);
        Set<String> fWords = extractKeywords(faqQuestion);
        if (fWords.isEmpty()) return 0;
        long overlap = qWords.stream().filter(fWords::contains).count();
        return (double) overlap / fWords.size();
    }

    private Set<String> extractKeywords(String text) {
        // 简单分词：去除标点，按2-4字滑动窗口提取中文关键词
        Set<String> keywords = new HashSet<>();
        String cleaned = text.replaceAll("[\uff0c\u3002\uff1f\uff01\u3001\uff1b\uff1a\u201c\u201d\u2018\u2019\uff08\uff09\\s]+", " ").trim();
        // 按空格和常见虚词拆分
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            if (part.length() >= 2 && part.length() <= 10) {
                keywords.add(part);
            }
            // 2-gram
            for (int i = 0; i <= part.length() - 2; i++) {
                keywords.add(part.substring(i, i + 2));
            }
        }
        return keywords;
    }

    /**
     * 获取所有分类
     */
    @Cacheable(value = "faqCache", key = "'categories'")
    public List<String> listCategories() {
        return faqEntryRepository.findAllCategories();
    }

    /**
     * 新增FAQ
     */
    @Transactional
    @CacheEvict(value = "faqCache", allEntries = true)
    public FaqEntry create(FaqEntry entry) {
        entry.setEnabled(true);
        return faqEntryRepository.save(entry);
    }

    /**
     * 更新FAQ
     */
    @Transactional
    @CacheEvict(value = "faqCache", allEntries = true)
    public FaqEntry update(Long id, FaqEntry entry) {
        FaqEntry existing = faqEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
        existing.setQuestion(entry.getQuestion());
        existing.setAnswer(entry.getAnswer());
        existing.setCategory(entry.getCategory());
        existing.setTags(entry.getTags());
        return faqEntryRepository.save(existing);
    }

    /**
     * 禁用FAQ
     */
    @Transactional
    @CacheEvict(value = "faqCache", allEntries = true)
    public void disable(Long id) {
        FaqEntry existing = faqEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
        existing.setEnabled(false);
        faqEntryRepository.save(existing);
    }
}
