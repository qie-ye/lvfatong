package com.lvatong.lft.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ContractAnalysisResult {

    private String summary;
    private String overallRisk; // 低/中/高
    private List<ClauseAnalysis> clauses;
    private Double verificationScore;       // 规则-AI交叉验证置信度 (0.0-1.0)
    private List<String> conflictNotes;     // 规则与AI判断有冲突的条款描述

    @Data
    public static class ClauseAnalysis {
        private int index;
        private String title;
        private String content;
        private String riskLevel;    // 低/中/高
        private String riskCategory; // 条款缺陷/法律适用错误/表述歧义
        private String description;
        private String legalBasis;
        private String suggestion;
    }
}
