package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legal_cases", indexes = {
        @Index(name = "idx_case_type", columnList = "caseType"),
        @Index(name = "idx_court", columnList = "court"),
        @Index(name = "idx_year", columnList = "year")
})
public class LegalCase extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 100)
    private String caseNo;

    @Column(length = 50)
    private String caseType;

    @Column(length = 100)
    private String court;

    @Column(length = 20)
    private String year;

    @Column(length = 50)
    private String domain;

    @Column(length = 500)
    private String keywords;

    @Column(length = 50)
    private String province;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String facts;

    @Column(columnDefinition = "TEXT")
    private String ruling;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Column
    private Boolean vectorIndexed;
}
