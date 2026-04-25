package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.FeedbackStatsResponse;
import com.lvatong.lft.model.entity.AnswerFeedback;
import com.lvatong.lft.repository.AnswerFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final AnswerFeedbackRepository feedbackRepository;

    /**
     * 提交反馈，同一用户对同一消息只能提交一次
     */
    @Transactional
    public void submitFeedback(Long userId, Long sessionId, Integer messageIndex, AnswerFeedback.Rating rating) {
        feedbackRepository.findByUserIdAndSessionIdAndMessageIndex(userId, sessionId, messageIndex)
                .ifPresent(existing -> {
                    throw new BusinessException("已对该消息提交过反馈，不可重复提交");
                });

        AnswerFeedback feedback = new AnswerFeedback();
        feedback.setUserId(userId);
        feedback.setSessionId(sessionId);
        feedback.setMessageIndex(messageIndex);
        feedback.setRating(rating);
        feedbackRepository.save(feedback);

        log.info("[Feedback] userId={} sessionId={} messageIndex={} rating={}",
                userId, sessionId, messageIndex, rating);
    }

    /**
     * 获取全局好评统计
     */
    public FeedbackStatsResponse getGlobalStats() {
        long goodCount = feedbackRepository.countByRating(AnswerFeedback.Rating.GOOD);
        long badCount  = feedbackRepository.countByRating(AnswerFeedback.Rating.BAD);
        return FeedbackStatsResponse.of(goodCount, badCount);
    }
}
