package com.lvatong.lft.rag;

import com.lvatong.lft.ai.EmbeddingService;
import com.lvatong.lft.model.entity.KnowledgeChunk;
import com.lvatong.lft.repository.KnowledgeChunkRepository;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorStoreService vectorStoreService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final EmbeddingService embeddingService;

    private volatile boolean milvusAvailable = true;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double VECTOR_WEIGHT = 0.6;
    private static final double FULLTEXT_WEIGHT = 0.4;
    private static final int DEFAULT_TOP_K = 5;

    /**
     * 混合检索：Milvus向量搜索 + MySQL FULLTEXT + RRF融合
     * 统一使用 chunk.id 作为 RRF 融合的 ID 空间
     */
    public List<SearchResult> search(String query, String docType, String lawDomain, int topK) {
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, KnowledgeChunk> chunkCache = new HashMap<>();
        int k = 60;

        // 全文检索 — 以 chunk.id 为基础
        try {
            String effectiveDocType = (docType != null && !docType.isBlank()) ? docType : null;
            String effectiveLawDomain = (lawDomain != null && !lawDomain.isBlank()) ? lawDomain : null;
            List<KnowledgeChunk> fulltextResults = knowledgeChunkRepository.fulltextSearch(
                    query, effectiveDocType, effectiveLawDomain, topK * 2);
            for (int i = 0; i < fulltextResults.size(); i++) {
                KnowledgeChunk chunk = fulltextResults.get(i);
                chunkCache.put(chunk.getId(), chunk);
                double rrfScore = FULLTEXT_WEIGHT / (k + i + 1);
                rrfScores.merge(chunk.getId(), rrfScore, Double::sum);
            }
        } catch (Exception e) {
            log.warn("Fulltext search failed: {}", e.getMessage());
        }

        // 向量检索 — Milvus不可用时跳过，避免浪费embedding API调用
        if (milvusAvailable) {
            try {
                List<Float> queryVector = embeddingService.embed(query);
                String filterExpr = buildFilterExpr(docType, lawDomain);
                List<SearchResp.SearchResult> vectorResults = vectorStoreService.search(queryVector, topK * 2, filterExpr);
                if (vectorResults.isEmpty() && !rrfScores.isEmpty()) {
                    // Milvus返回空但全文有结果，可能Milvus未索引数据，标记为降级
                    log.debug("Milvus returned empty, may not be indexed yet");
                }
                for (int i = 0; i < vectorResults.size(); i++) {
                    SearchResp.SearchResult result = vectorResults.get(i);
                    Long docId = ((Number) result.getEntity().get("document_id")).longValue();
                    String content = (String) result.getEntity().get("content");
                    // 查找该 document_id 下匹配的 chunk
                    List<KnowledgeChunk> docChunks = knowledgeChunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId);
                    KnowledgeChunk bestMatch = findBestChunk(docChunks, content);
                    if (bestMatch != null) {
                        chunkCache.put(bestMatch.getId(), bestMatch);
                        double rrfScore = VECTOR_WEIGHT / (k + i + 1);
                        rrfScores.merge(bestMatch.getId(), rrfScore, Double::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("Vector search failed, falling back to fulltext only: {}", e.getMessage());
                milvusAvailable = false;
                // 5分钟后重试Milvus连接
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        milvusAvailable = true;
                        log.info("Milvus availability reset, will retry on next search");
                    }
                }, 5 * 60 * 1000);
            }
        } else {
            log.debug("Milvus unavailable, using fulltext-only search");
        }

        // RRF排序
        List<Map.Entry<Long, Double>> sorted = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toList());

        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sorted) {
            KnowledgeChunk chunk = chunkCache.get(entry.getKey());
            if (chunk != null) {
                String[] meta = extractMeta(chunk.getMetadata());
                results.add(new SearchResult(chunk.getContent(), chunk.getDocType(),
                        chunk.getLawDomain(), entry.getValue(), meta[0], meta[1]));
            }
        }

        log.debug("Hybrid search returned {} results for query: {}", results.size(),
                query.length() > 50 ? query.substring(0, 50) + "..." : query);
        return results;
    }

    private KnowledgeChunk findBestChunk(List<KnowledgeChunk> chunks, String content) {
        if (chunks.isEmpty()) return null;
        if (content == null || content.isBlank()) return chunks.get(0);
        // 选择与向量存储 content 前缀最匹配的 chunk
        String prefix = content.length() > 100 ? content.substring(0, 100) : content;
        for (KnowledgeChunk chunk : chunks) {
            if (chunk.getContent().startsWith(prefix)) {
                return chunk;
            }
        }
        return chunks.get(0);
    }

    public List<SearchResult> search(String query, int topK) {
        return search(query, null, null, topK);
    }

    private String buildFilterExpr(String docType, String lawDomain) {
        List<String> conditions = new ArrayList<>();
        if (docType != null && !docType.isBlank()) {
            conditions.add("doc_type == \"" + docType + "\"");
        }
        if (lawDomain != null && !lawDomain.isBlank()) {
            conditions.add("law_domain == \"" + lawDomain + "\"");
        }
        return String.join(" and ", conditions);
    }

    public record SearchResult(String content, String docType, String lawDomain, double score,
                                  String sourceTitle, String sourceUrl) {
        public SearchResult(String content, String docType, String lawDomain, double score) {
            this(content, docType, lawDomain, score, null, null);
        }
    }

    private String[] extractMeta(String metadata) {
        String title = null;
        String url   = null;
        if (metadata != null && !metadata.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(metadata);
                title = node.path("sourceTitle").asText(null);
                url   = node.path("sourceUrl").asText(null);
            } catch (Exception ignored) {}
        }
        return new String[]{title, url};
    }
}
