package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, Long> {
    List<ContractDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
}
