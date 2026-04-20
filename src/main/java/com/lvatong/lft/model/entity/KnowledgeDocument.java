package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocType docType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "vector_indexed", nullable = false)
    private Boolean vectorIndexed = false;

    public enum DocType {
        LAW, CASE, CONTRACT_TEMPLATE, LEGAL_KNOWLEDGE, PRACTICE_GUIDE
    }
}
