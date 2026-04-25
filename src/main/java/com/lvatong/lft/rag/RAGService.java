package com.lvatong.lft.rag;

import com.alibaba.fastjson.JSONObject;
import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.EmbeddingService;
import com.lvatong.lft.ai.PromptTemplateService;
import com.lvatong.lft.model.entity.KnowledgeChunk;
import com.lvatong.lft.model.entity.KnowledgeDocument;
import com.lvatong.lft.repository.KnowledgeChunkRepository;
import com.lvatong.lft.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final HybridSearchService hybridSearchService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    @Lazy
    private final ChatService chatService;
    private final PromptTemplateService promptTemplateService;

    private static final int MAX_CONTEXT_TOKENS = 4096;
    private static final int CHARS_PER_TOKEN = 2;

    /**
     * 文档入库：分块→Embedding→Milvus存储
     */
    @Transactional
    public void ingestDocument(Long documentId) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<String> chunks = documentChunker.chunk(doc.getContent());
        log.info("Document {} chunked into {} pieces", documentId, chunks.size());

        List<JSONObject> milvusRows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);

            KnowledgeChunk chunkEntity = new KnowledgeChunk();
            chunkEntity.setDocumentId(documentId);
            chunkEntity.setChunkIndex(i);
            chunkEntity.setContent(chunkContent);
            chunkEntity.setDocType(doc.getDocType() != null ? doc.getDocType().name() : null);
            chunkEntity.setLawDomain(doc.getLawDomain());
            chunkEntity.setMetadata(buildChunkMetadata(doc));
            chunkEntity = knowledgeChunkRepository.save(chunkEntity);

            List<Float> embedding = embeddingService.embed(chunkContent);

            JSONObject row = new JSONObject();
            row.put("document_id", documentId);
            row.put("content", chunkContent.length() > 2000 ? chunkContent.substring(0, 2000) : chunkContent);
            row.put("doc_type", doc.getDocType() != null ? doc.getDocType().name() : "");
            row.put("law_domain", doc.getLawDomain() != null ? doc.getLawDomain() : "");
            row.put("embedding", embedding);
            milvusRows.add(row);

            if (milvusRows.size() >= 32) {
                vectorStoreService.batchInsert(milvusRows);
                milvusRows.clear();
            }
        }
        if (!milvusRows.isEmpty()) {
            vectorStoreService.batchInsert(milvusRows);
        }

        doc.setVectorIndexed(true);
        knowledgeDocumentRepository.save(doc);
        log.info("Document {} ingested successfully with {} chunks", documentId, chunks.size());
    }

    /**
     * 检索并构建上下文
     */
    public String retrieveAndBuildContext(String query, String docType, String lawDomain, int topK) {
        List<HybridSearchService.SearchResult> results = hybridSearchService.search(query, docType, lawDomain, topK);
        return buildContext(results);
    }

    public String retrieveAndBuildContext(String query, int topK) {
        return retrieveAndBuildContext(query, null, null, topK);
    }

    /**
     * 增强版检索：Query Rewriting + Iterative Retrieval + Context Compression
     */
    public String retrieveAndBuildContextEnhanced(String query, String docType, String lawDomain, int topK) {
        // Step 1: Query Rewriting
        String searchQuery = query;
        try {
            String rewrittenQuery = rewriteQuery(query);
            if (rewrittenQuery != null && !rewrittenQuery.isBlank()) {
                searchQuery = rewrittenQuery;
                log.info("Query rewritten: [{}] → [{}]", query, rewrittenQuery);
            }
        } catch (Exception e) {
            log.warn("Query rewriting failed, using original query: {}", e.getMessage());
        }

        // Step 2: First retrieval
        List<HybridSearchService.SearchResult> results = hybridSearchService.search(searchQuery, docType, lawDomain, topK);

        // Fallback: if rewritten query yields fewer results than original, use original
        if (results.size() < 2 && !searchQuery.equals(query)) {
            log.info("Rewritten query yielded too few results, falling back to original");
            List<HybridSearchService.SearchResult> originalResults = hybridSearchService.search(query, docType, lawDomain, topK);
            if (originalResults.size() > results.size()) {
                results = originalResults;
            }
        }

        // Step 3: Iterative Retrieval (if first round insufficient)
        if (results.size() < 2) {
            try {
                String supplementQuery = generateSupplementQuery(query, results);
                if (supplementQuery != null && !supplementQuery.isBlank()) {
                    log.info("Supplement query generated: [{}]", supplementQuery);
                    List<HybridSearchService.SearchResult> supplementResults =
                            hybridSearchService.search(supplementQuery, docType, lawDomain, topK);
                    results = mergeResults(results, supplementResults, topK);
                }
            } catch (Exception e) {
                log.warn("Supplement query generation failed, using first-round results: {}", e.getMessage());
            }
        }

        // Step 4: Context Compression
        return compressContext(query, results);
    }

    /**
     * 查询改写：调用 glm-4-flash 将口语化问题转为法律检索关键词串
     */
    private String rewriteQuery(String originalQuery) {
        String prompt = promptTemplateService.buildQueryRewritePrompt(originalQuery);
        String result = chatService.simpleChat(prompt, "glm-4-flash", 0.3, 64);
        return result != null ? result.trim() : null;
    }

    /**
     * 生成补充查询关键词
     */
    private String generateSupplementQuery(String originalQuery, List<HybridSearchService.SearchResult> firstResults) {
        String summary = firstResults.isEmpty() ? "无"
                : firstResults.stream()
                        .map(r -> r.content().length() > 100 ? r.content().substring(0, 100) : r.content())
                        .collect(Collectors.joining("\n---\n"));
        String prompt = promptTemplateService.buildSupplementQueryPrompt(originalQuery, summary);
        String result = chatService.simpleChat(prompt, "glm-4-flash", 0.3, 64);
        return result != null ? result.trim() : null;
    }

    /**
     * 合并两轮检索结果，按 score 去重后取 topK
     */
    private List<HybridSearchService.SearchResult> mergeResults(
            List<HybridSearchService.SearchResult> first,
            List<HybridSearchService.SearchResult> second,
            int topK) {
        Map<String, HybridSearchService.SearchResult> deduped = new LinkedHashMap<>();
        for (HybridSearchService.SearchResult r : first) {
            deduped.putIfAbsent(r.content(), r);
        }
        for (HybridSearchService.SearchResult r : second) {
            deduped.putIfAbsent(r.content(), r);
        }
        return deduped.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 上下文压缩（方案A）：按关键词命中密度重排后截断
     */
    private String compressContext(String query, List<HybridSearchService.SearchResult> results) {
        String[] keywords = query.toLowerCase().split("[\\s，。？！、,\\.\\?!]+");
        List<HybridSearchService.SearchResult> sorted = results.stream()
                .sorted((a, b) -> {
                    long scoreA = Arrays.stream(keywords)
                            .filter(kw -> kw.length() > 1 && a.content().toLowerCase().contains(kw))
                            .count();
                    long scoreB = Arrays.stream(keywords)
                            .filter(kw -> kw.length() > 1 && b.content().toLowerCase().contains(kw))
                            .count();
                    if (scoreB != scoreA) return Long.compare(scoreB, scoreA);
                    return Double.compare(b.score(), a.score());
                })
                .collect(Collectors.toList());
        return buildContext(sorted);
    }

    /**
     * 构建上下文（max 4096 tokens），附带来源引用
     */
    private String buildContext(List<HybridSearchService.SearchResult> results) {
        StringBuilder context = new StringBuilder();
        List<String> citations = new ArrayList<>();
        int usedTokens = 0;
        int idx = 1;
        for (HybridSearchService.SearchResult result : results) {
            int tokens = result.content().length() / CHARS_PER_TOKEN;
            if (usedTokens + tokens > MAX_CONTEXT_TOKENS) break;
            String refMark = result.sourceTitle() != null ? "[" + idx + "]" : "";
            context.append(result.content()).append(refMark).append("\n\n");
            usedTokens += tokens;
            if (result.sourceTitle() != null) {
                String cite = "[" + idx + "] " + result.sourceTitle();
                if (result.sourceUrl() != null) cite += "  →  " + result.sourceUrl();
                citations.add(cite);
                idx++;
            }
        }
        if (!citations.isEmpty()) {
            context.append("\n\n【参考法律依据】\n").append(String.join("\n", citations));
        }
        return context.toString().trim();
    }

    private String buildChunkMetadata(KnowledgeDocument doc) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode node = om.createObjectNode();
            node.put("sourceTitle", doc.getTitle());
            if (doc.getSourceUrl() != null) node.put("sourceUrl", doc.getSourceUrl());
            if (doc.getSource() != null)    node.put("source",    doc.getSource());
            return om.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
