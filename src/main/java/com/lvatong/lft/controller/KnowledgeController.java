package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.knowledge.CaseDataImportService;
import com.lvatong.lft.knowledge.FaqService;
import com.lvatong.lft.knowledge.NpcLawFetchService;
import com.lvatong.lft.model.entity.FaqEntry;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.model.dto.SearchRequest;
import com.lvatong.lft.repository.KnowledgeDocumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "法律知识库", description = "法条查询、常见问题、知识导入")
public class KnowledgeController {

    private final RAGService ragService;
    private final HybridSearchService hybridSearchService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final FaqService faqService;
    private final NpcLawFetchService npcLawFetchService;
    private final CaseDataImportService caseDataImportService;

    @GetMapping("/laws")
    @Operation(summary = "法律条文查询")
    public ApiResult<List<HybridSearchService.SearchResult>> searchLaws(@RequestParam("query") String query,
                                                                         @RequestParam(name = "docType", required = false) String docType,
                                                                         @RequestParam(name = "lawDomain", required = false) String lawDomain,
                                                                         @RequestParam(name = "topK", defaultValue = "5") int topK) {
        return ApiResult.success(hybridSearchService.search(query, docType, lawDomain, topK));
    }

    @PostMapping("/search")
    @Operation(summary = "知识库检索")
    public ApiResult<List<HybridSearchService.SearchResult>> search(@RequestBody SearchRequest request) {
        return ApiResult.success(hybridSearchService.search(request.getQuery(), request.getDocType(), request.getLawDomain(), request.getTopK()));
    }

    @PostMapping("/ingest/{documentId}")
    @Operation(summary = "触发文档入库（分块+Embedding+向量存储）")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<String> ingestDocument(@PathVariable("documentId") Long documentId) {
        ragService.ingestDocument(documentId);
        return ApiResult.success("文档 " + documentId + " 入库完成");
    }

    @GetMapping("/faq")
    @Operation(summary = "常见问题列表")
    public ApiResult<List<FaqEntry>> getFaq(@RequestParam(name = "category", required = false) String category) {
        return ApiResult.success(faqService.listByCategory(category));
    }

    @GetMapping("/faq/search")
    @Operation(summary = "FAQ搜索")
    public ApiResult<List<FaqEntry>> searchFaq(@RequestParam("query") String query,
                                               @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ApiResult.success(faqService.search(query, limit));
    }

    @GetMapping("/faq/categories")
    @Operation(summary = "FAQ分类列表")
    public ApiResult<List<String>> getFaqCategories() {
        return ApiResult.success(faqService.listCategories());
    }

    @PostMapping("/faq")
    @Operation(summary = "新增FAQ")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<FaqEntry> createFaq(@RequestBody FaqEntry entry) {
        return ApiResult.success(faqService.create(entry));
    }

    @PutMapping("/faq/{id}")
    @Operation(summary = "更新FAQ")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<FaqEntry> updateFaq(@PathVariable("id") Long id, @RequestBody FaqEntry entry) {
        return ApiResult.success(faqService.update(id, entry));
    }

    @DeleteMapping("/faq/{id}")
    @Operation(summary = "禁用FAQ")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<String> disableFaq(@PathVariable("id") Long id) {
        faqService.disable(id);
        return ApiResult.success("FAQ已禁用");
    }

    // ──────────────────────────────────────────────────────────
    // 数据同步管理（ADMIN）
    // ──────────────────────────────────────────────────────────

    @PostMapping("/sync/npc-laws")
    @Operation(summary = "从国家法律法规数据库抓取法律条文",
               description = "type: flfg=法律法规 sfjs=司法解释 xzfg=行政法规；maxCount 建议≤50")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> fetchNpcLaws(
            @RequestParam(name = "type", defaultValue = "flfg") String type,
            @RequestParam(name = "maxCount", defaultValue = "20")   int maxCount) {
        int imported = npcLawFetchService.fetchAndImport(type, maxCount);
        return ApiResult.success(Map.of("imported", imported,
                "message", "已导入 " + imported + " 条，可调用 /ingest-pending 触发向量化"));
    }

    @PostMapping("/sync/import-cases")
    @Operation(summary = "批量导入案例（支持 CAIL 每行JSON 或 标准JSON数组）",
               description = "CAIL数据集下载：https://github.com/china-ai-law-challenge/CAIL2019")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> importCases(
            @RequestParam("file")                   MultipartFile file,
            @RequestParam(name = "caseType", defaultValue = "刑事")    String caseType,
            @RequestParam(name = "maxCount", defaultValue = "500")     int maxCount) throws Exception {
        int imported = caseDataImportService.importFromFile(file, caseType, maxCount);
        return ApiResult.success(Map.of("imported", imported));
    }

    @PostMapping("/sync/ingest-pending")
    @Operation(summary = "对所有未向量化文档执行分块+Embedding+Milvus入库",
               description = "建议在 Milvus 启动后执行；每个文档调用一次 BGE-M3 Embedding")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> ingestPending() {
        int count = npcLawFetchService.ingestPending();
        return ApiResult.success(Map.of("ingested", count,
                "message", "成功向量化 " + count + " 个文档"));
    }

    @PostMapping("/sync/ingest-all-chunks")
    @Operation(summary = "对 knowledge_chunks 已有数据触发全文索引（无需 Milvus）",
               description = "已有的 chunk 数据直接支持 MySQL FULLTEXT 搜索，无需额外操作")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> getChunkStats() {
        long docCount   = knowledgeDocumentRepository.count();
        long pendingCount = knowledgeDocumentRepository.findByVectorIndexedFalse().size();
        return ApiResult.success(Map.of(
                "totalDocuments",   docCount,
                "pendingIngest",    pendingCount,
                "tip", "调用 /sync/npc-laws 导入法条，/sync/ingest-pending 触发向量化"
        ));
    }
}
