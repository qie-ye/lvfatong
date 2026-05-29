package com.lvatong.lft.mq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long documentId;
    private Long userId;
    private String filePath;
    private String fileName;
    private TaskType taskType;
    private LocalDateTime createdAt;

    public enum TaskType {
        CONTRACT_ANALYSIS,
        DOCUMENT_PARSING,
        RISK_ASSESSMENT
    }
}
