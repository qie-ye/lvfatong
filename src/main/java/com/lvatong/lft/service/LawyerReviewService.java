package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.CreateReviewRequest;
import com.lvatong.lft.model.dto.LawyerReviewResponse;
import com.lvatong.lft.model.entity.LawyerProfile;
import com.lvatong.lft.model.entity.LawyerReview;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.LawyerProfileRepository;
import com.lvatong.lft.repository.LawyerReviewRepository;
import com.lvatong.lft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawyerReviewService {

    private final LawyerReviewRepository lawyerReviewRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final UserRepository userRepository;

    /**
     * 创建评价（每个用户对同一律师只能评价一次）
     */
    @Transactional
    @CacheEvict(value = "lawyerListCache", allEntries = true)
    public LawyerReviewResponse createReview(Long userId, CreateReviewRequest request) {
        // 检查律师是否存在
        LawyerProfile lawyer = lawyerProfileRepository.findById(request.getLawyerId())
                .orElseThrow(() -> new BusinessException("律师不存在"));

        // 检查是否已评价
        lawyerReviewRepository.findByUserIdAndLawyerId(userId, request.getLawyerId())
                .ifPresent(r -> { throw new BusinessException("您已评价过该律师，不能重复评价"); });

        LawyerReview review = new LawyerReview();
        review.setUserId(userId);
        review.setLawyerId(request.getLawyerId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setServiceType(request.getServiceType());
        review = lawyerReviewRepository.save(review);

        // 更新律师评分
        updateLawyerRating(request.getLawyerId());

        LawyerReviewResponse response = LawyerReviewResponse.from(review);
        // 填充用户名
        userRepository.findById(userId).ifPresent(u -> response.setUsername(u.getUsername()));
        return response;
    }

    /**
     * 获取律师的评价列表
     */
    public List<LawyerReviewResponse> getLawyerReviews(Long lawyerId) {
        return lawyerReviewRepository.findByLawyerIdOrderByCreatedAtDesc(lawyerId)
                .stream()
                .map(r -> {
                    LawyerReviewResponse resp = LawyerReviewResponse.from(r);
                    userRepository.findById(r.getUserId()).ifPresent(u -> resp.setUsername(u.getUsername()));
                    return resp;
                })
                .toList();
    }

    /**
     * 获取用户的评价列表
     */
    public List<LawyerReviewResponse> getUserReviews(Long userId) {
        return lawyerReviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(LawyerReviewResponse::from)
                .toList();
    }

    /**
     * 协同过滤推荐
     */
    @Cacheable(value = "lawyerListCache", key = "'cf:' + #userId")
    public List<Long> getCollaborativeRecommendations(Long userId, int limit) {
        List<Object[]> recommendations = lawyerReviewRepository.findCollaborativeRecommendations(userId);
        return recommendations.stream()
                .limit(limit)
                .map(row -> ((Number) row[0]).longValue())
                .toList();
    }

    /**
     * 更新律师评分聚合
     */
    private void updateLawyerRating(Long lawyerId) {
        Double avgRating = lawyerReviewRepository.getAverageRating(lawyerId);
        Long reviewCount = lawyerReviewRepository.getReviewCount(lawyerId);

        lawyerProfileRepository.findById(lawyerId).ifPresent(lawyer -> {
            if (avgRating != null) {
                lawyer.setRating(Math.round(avgRating * 10) / 10.0);
            }
            lawyer.setConsultationCount(reviewCount != null ? reviewCount.intValue() : 0);
            lawyerProfileRepository.save(lawyer);
        });
    }
}
