package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.LegalCaseResponse;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "案例检索", description = "法律案例全文检索、语义搜索、AI分析")
public class CaseController {

    private final CaseService caseService;

    @GetMapping("/{id}")
    @Operation(summary = "获取案例详情")
    public ApiResult<LegalCaseResponse> getCase(@PathVariable("id") Long id) {
        return ApiResult.success(caseService.getCase(id));
    }

    @GetMapping
    @Operation(summary = "搜索案例")
    public ApiResult<Page<LegalCaseResponse>> searchCases(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "caseType", required = false) String caseType,
            @RequestParam(name = "domain", required = false) String domain,
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResult.success(caseService.searchCases(keyword, caseType, domain, year, page, size));
    }

    @PostMapping("/semantic-search")
    @Operation(summary = "AI语义检索案例")
    public ApiResult<List<HybridSearchService.SearchResult>> semanticSearch(
            @RequestParam("query") String query,
            @RequestParam(name = "domain", required = false) String domain,
            @RequestParam(name = "topK", defaultValue = "5") int topK) {
        return ApiResult.success(caseService.semanticSearch(query, domain, topK));
    }
}
