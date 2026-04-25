package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.LegalOpinion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LegalOpinionResponse {
    private Long id;
    private String title;
    private String domain;
    private String question;
    private String facts;
    private String analysis;
    private String conclusion;
    private String legalBasis;
    private String suggestions;
    private String status;
    private String model;
    private LocalDateTime createdAt;

    public static LegalOpinionResponse from(LegalOpinion op) {
        return LegalOpinionResponse.builder()
                .id(op.getId())
                .title(op.getTitle())
                .domain(op.getDomain())
                .question(op.getQuestion())
                .facts(op.getFacts())
                .analysis(op.getAnalysis())
                .conclusion(op.getConclusion())
                .legalBasis(op.getLegalBasis())
                .suggestions(op.getSuggestions())
                .status(op.getStatus())
                .model(op.getModel())
                .createdAt(op.getCreatedAt())
                .build();
    }

    public static LegalOpinionResponse summaryFrom(LegalOpinion op) {
        return LegalOpinionResponse.builder()
                .id(op.getId())
                .title(op.getTitle())
                .domain(op.getDomain())
                .status(op.getStatus())
                .createdAt(op.getCreatedAt())
                .build();
    }
}
