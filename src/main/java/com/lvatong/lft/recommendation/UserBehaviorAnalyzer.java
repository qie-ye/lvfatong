package com.lvatong.lft.recommendation;

import com.lvatong.lft.model.entity.recommendation.UserBehavior;
import com.lvatong.lft.model.entity.recommendation.UserPreference;
import com.lvatong.lft.repository.recommendation.UserBehaviorRepository;
import com.lvatong.lft.repository.recommendation.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为分析服务
 *
 * 负责：
 * 1. 记录用户行为
 * 2. 分析用户偏好
 * 3. 更新用户画像
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorAnalyzer {

    private final UserBehaviorRepository behaviorRepository;
    private final UserPreferenceRepository preferenceRepository;

    /**
     * 记录用户行为
     */
    @Transactional
    public void recordBehavior(Long userId, UserBehavior.ActionType actionType,
                                UserBehavior.TargetType targetType, Long targetId,
                                String queryText, String domain, Integer durationSeconds) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType(actionType);
        behavior.setTargetType(targetType);
        behavior.setTargetId(targetId);
        behavior.setQueryText(queryText);
        behavior.setDomain(domain);
        behavior.setDurationSeconds(durationSeconds);

        behaviorRepository.save(behavior);
        log.debug("Recorded behavior: userId={}, action={}, target={}", userId, actionType, targetType);

        // 异步更新用户画像
        updateUserPreference(userId);
    }

    /**
     * 记录搜索行为
     */
    public void recordSearch(Long userId, String query, String domain) {
        recordBehavior(userId, UserBehavior.ActionType.SEARCH, UserBehavior.TargetType.CHAT,
                null, query, domain, null);
    }

    /**
     * 记录查看行为
     */
    public void recordView(Long userId, UserBehavior.TargetType targetType, Long targetId, String domain) {
        recordBehavior(userId, UserBehavior.ActionType.VIEW, targetType, targetId,
                null, domain, null);
    }

    /**
     * 记录反馈行为
     */
    public void recordFeedback(Long userId, Long targetId, String rating) {
        recordBehavior(userId, UserBehavior.ActionType.FEEDBACK, UserBehavior.TargetType.CHAT,
                targetId, null, null, null);
    }

    /**
     * 更新用户偏好画像
     */
    @Transactional
    public void updateUserPreference(Long userId) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(new UserPreference());

        if (preference.getUserId() == null) {
            preference.setUserId(userId);
        }

        // 分析偏好领域
        List<Object[]> topDomains = behaviorRepository.findTopDomainsByUserId(userId, PageRequest.of(0, 5));
        if (!topDomains.isEmpty()) {
            List<String> domains = topDomains.stream()
                    .map(row -> (String) row[0])
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            preference.setPreferredDomains(String.join(",", domains));
        }

        // 计算查询频率
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long queryCount = behaviorRepository.countByUserIdSince(userId, since);
        preference.setQueryFrequency((int) queryCount);

        // 更新最后活跃时间
        preference.setLastActiveAt(LocalDateTime.now());

        // 判断专业水平
        if (queryCount > 100) {
            preference.setExpertiseLevel(UserPreference.ExpertiseLevel.EXPERT);
        } else if (queryCount > 30) {
            preference.setExpertiseLevel(UserPreference.ExpertiseLevel.INTERMEDIATE);
        } else {
            preference.setExpertiseLevel(UserPreference.ExpertiseLevel.BEGINNER);
        }

        preferenceRepository.save(preference);
        log.debug("Updated preference for user {}: domains={}, frequency={}, level={}",
                userId, preference.getPreferredDomains(), queryCount, preference.getExpertiseLevel());
    }

    /**
     * 获取用户偏好
     */
    public UserPreference getUserPreference(Long userId) {
        return preferenceRepository.findByUserId(userId).orElse(null);
    }

    /**
     * 获取用户偏好领域列表
     */
    public List<String> getPreferredDomains(Long userId) {
        UserPreference preference = getUserPreference(userId);
        if (preference == null || preference.getPreferredDomains() == null) {
            return List.of();
        }
        return Arrays.asList(preference.getPreferredDomains().split(","));
    }

    /**
     * 获取用户最近行为
     */
    public List<UserBehavior> getRecentBehaviors(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return behaviorRepository.findRecentBehaviors(userId, since);
    }

    /**
     * 获取用户查询历史
     */
    public List<String> getUserQueryHistory(Long userId, int limit) {
        List<UserBehavior> behaviors = behaviorRepository.findByUserIdAndActionType(
                userId, UserBehavior.ActionType.SEARCH);
        return behaviors.stream()
                .limit(limit)
                .map(UserBehavior::getQueryText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否为新用户
     */
    public boolean isNewUser(Long userId) {
        return !preferenceRepository.existsByUserId(userId);
    }

    /**
     * 获取用户活跃度分数
     */
    public double getActivityScore(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long count = behaviorRepository.countByUserIdSince(userId, since);

        if (count > 50) return 1.0;
        if (count > 20) return 0.8;
        if (count > 10) return 0.6;
        if (count > 5) return 0.4;
        return 0.2;
    }
}
