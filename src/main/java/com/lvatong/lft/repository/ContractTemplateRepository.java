package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    List<ContractTemplate> findByEnabledTrueOrderByCategory();
    List<ContractTemplate> findByCategory(String category);
}
