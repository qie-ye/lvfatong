package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "doc_type", length = 50)
    private String docType;

    @Column(name = "law_domain", length = 50)
    private String lawDomain;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
