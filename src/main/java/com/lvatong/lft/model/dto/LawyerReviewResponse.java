package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.LawyerReview;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LawyerReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long lawyerId;
    private Integer rating;
    private String comment;
    private String serviceType;
    private LocalDateTime createdAt;

    public static LawyerReviewResponse from(LawyerReview review) {
        return LawyerReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .lawyerId(review.getLawyerId())
                .rating(review.getRating())
                .comment(review.getComment())
                .serviceType(review.getServiceType())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
