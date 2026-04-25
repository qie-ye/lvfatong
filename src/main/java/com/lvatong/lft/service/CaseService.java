package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.LegalCaseResponse;
import com.lvatong.lft.model.entity.LegalCase;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.repository.LegalCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final LegalCaseRepository legalCaseRepository;
    private final HybridSearchService hybridSearchService;

    /**
     * 获取案例详情
     */
    @Cacheable(value = "caseSearchCache", key = "'case:' + #id")
    public LegalCaseResponse getCase(Long id) {
        LegalCase lc = legalCaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("案例不存在"));
        return LegalCaseResponse.from(lc);
    }

    /**
     * 关键词搜索案例（MySQL全文 + 向量混合）
     */
    public Page<LegalCaseResponse> searchCases(String keyword, String caseType, String domain,
                                                 String year, int page, int size) {
        Page<LegalCase> results;

        if (keyword != null && !keyword.isBlank()) {
            // 优先使用全文检索
            results = legalCaseRepository.fullTextSearch(keyword, PageRequest.of(page, size));
        } else if (domain != null && !domain.isBlank() && caseType != null && !caseType.isBlank()) {
            results = legalCaseRepository.findByDomainAndCaseType(domain, caseType, PageRequest.of(page, size));
        } else if (domain != null && !domain.isBlank()) {
            results = legalCaseRepository.findByDomain(domain, PageRequest.of(page, size));
        } else if (caseType != null && !caseType.isBlank()) {
            results = legalCaseRepository.findByCaseType(caseType, PageRequest.of(page, size));
        } else if (year != null && !year.isBlank()) {
            results = legalCaseRepository.findByYear(year, PageRequest.of(page, size));
        } else {
            results = legalCaseRepository.findAll(PageRequest.of(page, size));
        }

        return results.map(LegalCaseResponse::summaryFrom);
    }

    /**
     * AI语义检索案例（基于向量相似度）
     */
    public List<HybridSearchService.SearchResult> semanticSearch(String query, String domain, int topK) {
        return hybridSearchService.search(query, "CASE", domain, topK);
    }

    /**
     * 导入案例数据
     */
    @Transactional
    public LegalCaseResponse importCase(LegalCase legalCase) {
        legalCase.setVectorIndexed(false);
        legalCase = legalCaseRepository.save(legalCase);
        log.info("Case imported: id={} title={}", legalCase.getId(), legalCase.getTitle());
        return LegalCaseResponse.from(legalCase);
    }

    /**
     * 获取待向量化案例列表
     */
    public List<LegalCase> getPendingVectorIndexCases() {
        return legalCaseRepository.findByVectorIndexedFalse();
    }

    /**
     * 标记案例已向量化
     */
    @Transactional
    @CacheEvict(value = "caseSearchCache", key = "'case:' + #caseId")
    public void markVectorIndexed(Long caseId) {
        legalCaseRepository.findById(caseId).ifPresent(lc -> {
            lc.setVectorIndexed(true);
            legalCaseRepository.save(lc);
        });
    }
}
