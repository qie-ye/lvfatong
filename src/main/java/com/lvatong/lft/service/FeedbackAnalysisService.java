package com.lvatong.lft.service;

import com.lvatong.lft.model.entity.AnswerFeedback;
import com.lvatong.lft.repository.AnswerFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackAnalysisService {

    private final AnswerFeedbackRepository feedbackRepository;

    /**
     * 获取高频点踩问题列表
     * 按问题类型和意图类型聚合，返回需要优先优化的问题
     */
    public List<FrequentIssue> getFrequentIssues(int limit) {
        List<AnswerFeedback> badFeedbacks = feedbackRepository.findByRating(AnswerFeedback.Rating.BAD);
        
        // 按问题内容分组（去重后统计）
        Map<String, List<AnswerFeedback>> groupedByQuestion = badFeedbacks.stream()
                .filter(f -> f.getQuestion() != null && !f.getQuestion().isBlank())
                .collect(Collectors.groupingBy(AnswerFeedback::getQuestion));
        
        // 计算每个问题的出现次数，按次数排序
        return groupedByQuestion.entrySet().stream()
                .map(entry -> {
                    List<AnswerFeedback> feedbacks = entry.getValue();
                    AnswerFeedback sample = feedbacks.get(0);
                    return new FrequentIssue(
                            entry.getKey(),
                            sample.getAnswer(),
                            feedbacks.size(),
                            sample.getIntentType(),
                            extractIssueTags(feedbacks),
                            extractBadReasons(feedbacks)
                    );
                })
                .sorted(Comparator.comparingInt(FrequentIssue::count).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 按意图类型统计点踩分布
     */
    public Map<String, Long> getBadFeedbackByIntentType() {
        List<AnswerFeedback> badFeedbacks = feedbackRepository.findByRating(AnswerFeedback.Rating.BAD);
        
        return badFeedbacks.stream()
                .filter(f -> f.getIntentType() != null)
                .collect(Collectors.groupingBy(
                        AnswerFeedback::getIntentType,
                        Collectors.counting()
                ));
    }

    /**
     * 按问题标签统计点踩分布
     */
    public Map<String, Long> getBadFeedbackByIssueTags() {
        List<AnswerFeedback> badFeedbacks = feedbackRepository.findByRating(AnswerFeedback.Rating.BAD);
        
        Map<String, Long> tagCounts = new HashMap<>();
        for (AnswerFeedback feedback : badFeedbacks) {
            if (feedback.getIssueTags() != null && !feedback.getIssueTags().isBlank()) {
                String[] tags = feedback.getIssueTags().split("[,，]");
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tagCounts.merge(trimmedTag, 1L, Long::sum);
                    }
                }
            }
        }
        
        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 获取微调训练数据
     * 从点赞的反馈中提取高质量QA对
     */
    public List<TrainingData> getTrainingDataForFineTuning(int limit) {
        List<AnswerFeedback> goodFeedbacks = feedbackRepository.findByRating(AnswerFeedback.Rating.GOOD);
        
        return goodFeedbacks.stream()
                .filter(f -> f.getQuestion() != null && f.getAnswer() != null)
                .filter(f -> !f.getQuestion().isBlank() && !f.getAnswer().isBlank())
                .limit(limit)
                .map(f -> new TrainingData(
                        f.getQuestion(),
                        f.getAnswer(),
                        f.getIntentType()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取需要优先优化的问题（高频点踩 + 缺失回答）
     */
    public List<OptimizationPriority> getOptimizationPriorities(int limit) {
        List<FrequentIssue> frequentIssues = getFrequentIssues(limit * 2);
        
        return frequentIssues.stream()
                .map(issue -> {
                    // 计算优先级分数：出现次数 × 问题严重程度
                    double priorityScore = issue.count() * getIssueSeverity(issue.issueTags());
                    return new OptimizationPriority(
                            issue.question(),
                            issue.answer(),
                            issue.count(),
                            issue.intentType(),
                            issue.issueTags(),
                            issue.badReasons(),
                            priorityScore
                    );
                })
                .sorted(Comparator.comparingDouble(OptimizationPriority::priorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 提取问题标签
     */
    private Set<String> extractIssueTags(List<AnswerFeedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> f.getIssueTags() != null && !f.getIssueTags().isBlank())
                .flatMap(f -> Arrays.stream(f.getIssueTags().split("[,，]")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 提取点踩原因
     */
    private List<String> extractBadReasons(List<AnswerFeedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> f.getBadReason() != null && !f.getBadReason().isBlank())
                .map(AnswerFeedback::getBadReason)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据问题标签计算严重程度
     */
    private double getIssueSeverity(Set<String> issueTags) {
        if (issueTags == null || issueTags.isEmpty()) {
            return 1.0;
        }
        
        double severity = 1.0;
        for (String tag : issueTags) {
            if (tag.contains("幻觉") || tag.contains("编造")) {
                severity += 3.0; // 幻觉问题最严重
            } else if (tag.contains("法条错误") || tag.contains("引用错误")) {
                severity += 2.5;
            } else if (tag.contains("逻辑混乱") || tag.contains("前后矛盾")) {
                severity += 2.0;
            } else if (tag.contains("回答不完整") || tag.contains("遗漏")) {
                severity += 1.5;
            } else if (tag.contains("格式问题") || tag.contains("表述不清")) {
                severity += 1.0;
            }
        }
        return severity;
    }

    /**
     * 高频问题记录
     */
    public record FrequentIssue(
            String question,
            String answer,
            int count,
            String intentType,
            Set<String> issueTags,
            List<String> badReasons
    ) {}

    /**
     * 微调训练数据
     */
    public record TrainingData(
            String question,
            String answer,
            String intentType
    ) {}

    /**
     * 优化优先级记录
     */
    public record OptimizationPriority(
            String question,
            String answer,
            int count,
            String intentType,
            Set<String> issueTags,
            List<String> badReasons,
            double priorityScore
    ) {}
}
