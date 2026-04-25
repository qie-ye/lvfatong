package com.lvatong.lft.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractModificationSuggestion {
    private int clauseIndex;
    private String clauseTitle;
    private String riskLevel;
    private String originalContent;
    private String suggestion;
    private String legalBasis;
    private String aiModificationDetail;
}
