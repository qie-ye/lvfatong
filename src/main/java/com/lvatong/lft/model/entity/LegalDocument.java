package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legal_documents", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_doc_type", columnList = "docType")
})
public class LegalDocument extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 50)
    private String docType; // COMPLAINT, DEFENSE, ARBITRATION, PETITION, INDICTMENT, OTHER

    @Column(length = 50)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String facts;

    @Column(columnDefinition = "TEXT")
    private String claims;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    private String status; // GENERATING, COMPLETED, FAILED

    @Column(length = 50)
    private String model;
}
