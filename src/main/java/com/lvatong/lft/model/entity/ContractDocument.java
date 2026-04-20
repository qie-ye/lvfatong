package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contract_documents")
public class ContractDocument extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String filename;

    @Column(length = 50)
    private String fileType;

    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String parsedText;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStatus status;

    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    public enum AnalysisStatus {
        UPLOADED, PARSING, PARSED, ANALYZING, COMPLETED, FAILED
    }
}
