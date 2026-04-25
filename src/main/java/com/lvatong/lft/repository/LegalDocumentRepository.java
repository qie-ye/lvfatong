package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    List<LegalDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LegalDocument> findByUserIdAndDocType(Long userId, String docType);
}
