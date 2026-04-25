package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.LegalCase;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegalCaseResponse {
    private Long id;
    private String title;
    private String caseNo;
    private String caseType;
    private String court;
    private String year;
    private String domain;
    private String keywords;
    private String province;
    private String summary;
    private String facts;
    private String ruling;
    private String analysis;

    public static LegalCaseResponse from(LegalCase lc) {
        return LegalCaseResponse.builder()
                .id(lc.getId())
                .title(lc.getTitle())
                .caseNo(lc.getCaseNo())
                .caseType(lc.getCaseType())
                .court(lc.getCourt())
                .year(lc.getYear())
                .domain(lc.getDomain())
                .keywords(lc.getKeywords())
                .province(lc.getProvince())
                .summary(lc.getSummary())
                .facts(lc.getFacts())
                .ruling(lc.getRuling())
                .analysis(lc.getAnalysis())
                .build();
    }

    public static LegalCaseResponse summaryFrom(LegalCase lc) {
        return LegalCaseResponse.builder()
                .id(lc.getId())
                .title(lc.getTitle())
                .caseNo(lc.getCaseNo())
                .caseType(lc.getCaseType())
                .court(lc.getCourt())
                .year(lc.getYear())
                .domain(lc.getDomain())
                .keywords(lc.getKeywords())
                .province(lc.getProvince())
                .summary(lc.getSummary() != null && lc.getSummary().length() > 200
                        ? lc.getSummary().substring(0, 200) + "..." : lc.getSummary())
                .build();
    }
}
