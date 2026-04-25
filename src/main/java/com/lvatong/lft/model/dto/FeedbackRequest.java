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
}
