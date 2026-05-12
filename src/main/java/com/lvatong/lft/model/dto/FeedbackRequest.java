package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.AnswerFeedback;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    @Min(0)
    private Integer messageIndex;

    @NotNull
    private AnswerFeedback.Rating rating;

    /**
     * 点踩原因（可选，用户填写）
     */
    private String badReason;

    /**
     * 问题分类标签（可选，用户选择或系统自动标注）
     * 例如：法条错误、逻辑混乱、回答不完整、幻觉等
     */
    private String issueTags;
}
