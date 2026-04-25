package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByVectorIndexedFalse();
    List<KnowledgeDocument> findByDocType(KnowledgeDocument.DocType docType);
    boolean existsByTitle(String title);
}
