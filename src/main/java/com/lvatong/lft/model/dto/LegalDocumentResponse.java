package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.LegalDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LegalDocumentResponse {
    private Long id;
    private String title;
    private String docType;
    private String domain;
    private String facts;
    private String claims;
    private String content;
    private String status;
    private String model;
    private LocalDateTime createdAt;

    public static LegalDocumentResponse from(LegalDocument doc) {
        return LegalDocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .domain(doc.getDomain())
                .facts(doc.getFacts())
                .claims(doc.getClaims())
                .content(doc.getContent())
                .status(doc.getStatus())
                .model(doc.getModel())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public static LegalDocumentResponse summaryFrom(LegalDocument doc) {
        return LegalDocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .docType(doc.getDocType())
                .domain(doc.getDomain())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
