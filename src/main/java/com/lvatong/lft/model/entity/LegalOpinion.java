package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legal_opinions", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_domain", columnList = "domain")
})
public class LegalOpinion extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 50)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String facts;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Column(columnDefinition = "TEXT")
    private String legalBasis;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(length = 20)
    private String status; // GENERATING, COMPLETED, FAILED

    @Column(length = 50)
    private String model;
}
