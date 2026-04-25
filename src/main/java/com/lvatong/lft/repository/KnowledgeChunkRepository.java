package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Query(value = "SELECT c.* FROM knowledge_chunks c " +
            "WHERE MATCH(c.content) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
            "AND (:docType IS NULL OR c.doc_type = :docType) " +
            "AND (:lawDomain IS NULL OR c.law_domain = :lawDomain) " +
            "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> fulltextSearch(@Param("query") String query,
                                        @Param("docType") String docType,
                                        @Param("lawDomain") String lawDomain,
                                        @Param("limit") int limit);
}
