package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.FeedbackRequest;
import com.lvatong.lft.model.dto.FeedbackStatsResponse;
import com.lvatong.lft.model.entity.AnswerFeedback;
import com.lvatong.lft.model.entity.ChatMessage;
import com.lvatong.lft.repository.AnswerFeedbackRepository;
import com.lvatong.lft.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final AnswerFeedbackRepository feedbackRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 提交反馈，同一用户对同一消息只能提交一次
     * 保存完整上下文，用于后续分析和微调
     */
    @Transactional
    public void submitFeedback(Long userId, FeedbackRequest request) {
        // 检查是否已提交过反馈
        feedbackRepository.findByUserIdAndSessionIdAndMessageIndex(
                userId, request.getSessionId(), request.getMessageIndex())
                .ifPresent(existing -> {
                    throw new BusinessException("已对该消息提交过反馈，不可重复提交");
                });

        // 获取消息上下文
        String question = null;
        String answer = null;
        String intentType = null;
        
        try {
            List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(
                    request.getSessionId());
            
            int userMsgIndex = 0;
            int assistantMsgIndex = 0;
            
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.MessageRole.USER) {
                    if (userMsgIndex == request.getMessageIndex()) {
                        question = msg.getContent();
                    }
                    userMsgIndex++;
                } else if (msg.getRole() == ChatMessage.MessageRole.ASSISTANT) {
                    if (assistantMsgIndex == request.getMessageIndex()) {
                        answer = msg.getContent();
                    }
                    assistantMsgIndex++;
                }
            }
            
            // 根据问题内容简单推断意图类型
            intentType = inferIntentType(question);
            
        } catch (Exception e) {
            log.warn("Failed to fetch message context for feedback: {}", e.getMessage());
        }

        // 创建反馈记录
        AnswerFeedback feedback = new AnswerFeedback();
        feedback.setUserId(userId);
        feedback.setSessionId(request.getSessionId());
        feedback.setMessageIndex(request.getMessageIndex());
        feedback.setRating(request.getRating());
        feedback.setQuestion(question);
        feedback.setAnswer(answer);
        feedback.setIntentType(intentType);
        feedback.setBadReason(request.getBadReason());
        feedback.setIssueTags(request.getIssueTags());
        
        feedbackRepository.save(feedback);

        log.info("[Feedback] userId={} sessionId={} msgIdx={} rating={} intent={} badReason={}",
                userId, request.getSessionId(), request.getMessageIndex(), 
                request.getRating(), intentType, request.getBadReason());
    }

    /**
     * 根据问题内容推断意图类型
     */
    private String inferIntentType(String question) {
        if (question == null || question.isBlank()) {
            return "UNKNOWN";
        }
        
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("合同") || lowerQuestion.contains("签约") || lowerQuestion.contains("违约")) {
            return "CONTRACT_QUESTION";
        } else if (lowerQuestion.contains("案例") || lowerQuestion.contains("判决") || lowerQuestion.contains("法院")) {
            return "CASE_QUERY";
        } else if (lowerQuestion.contains("第") && lowerQuestion.contains("条")) {
            return "LAW_QUERY";
        } else if (lowerQuestion.contains("怎么办") || lowerQuestion.contains("如何") || lowerQuestion.contains("怎样")) {
            return "LEGAL_QA";
        } else {
            return "LEGAL_QA";
        }
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
