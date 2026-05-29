package com.lvatong.lft.rag;

import com.lvatong.lft.gpu.GpuServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reranker 服务
 *
 * 使用 GPU 推理服务的 BGE-Reranker 模型对检索结果进行重排序
 * 提升检索结果的相关性
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lvatong.gpu.enabled", havingValue = "true", matchIfMissing = true)
public class RerankerService {

    private final GpuServiceClient gpuServiceClient;

    /**
     * 对检索结果进行重排序
     *
     * @param query    查询文本
     * @param results  原始检索结果
     * @param topK     返回数量
     * @return 重排序后的结果
     */
    public List<HybridSearchService.SearchResult> rerank(
            String query,
            List<HybridSearchService.SearchResult> results,
            int topK) {

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        // 如果结果数量不超过 topK，直接返回
        if (results.size() <= topK) {
            return results;
        }

        try {
            // 提取文档内容
            List<String> documents = results.stream()
                    .map(HybridSearchService.SearchResult::content)
                    .collect(Collectors.toList());

            // 调用 GPU Reranker 服务
            List<GpuServiceClient.RerankResult> rerankResults = gpuServiceClient.rerank(
                    query, documents, topK);

            if (rerankResults == null || rerankResults.isEmpty()) {
                log.warn("Reranker returned empty results, using original order");
                return results.stream().limit(topK).collect(Collectors.toList());
            }

            // 根据 rerank 结果重新排序
            List<HybridSearchService.SearchResult> reranked = new ArrayList<>();
            for (GpuServiceClient.RerankResult rerankResult : rerankResults) {
                int originalIndex = rerankResult.getIndex();
                if (originalIndex >= 0 && originalIndex < results.size()) {
                    HybridSearchService.SearchResult original = results.get(originalIndex);
                    // 创建新的 SearchResult，使用 rerank 分数
                    reranked.add(new HybridSearchService.SearchResult(
                            original.content(),
                            original.docType(),
                            original.lawDomain(),
                            rerankResult.getScore(),
                            original.sourceTitle(),
                            original.sourceUrl()
                    ));
                }
            }

            log.debug("Reranked {} results to {} results", results.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.warn("Reranking failed, using original order: {}", e.getMessage());
            return results.stream().limit(topK).collect(Collectors.toList());
        }
    }

    /**
     * 计算单个查询-文档对的相关性分数
     */
    public double computeScore(String query, String document) {
        try {
            List<String> documents = List.of(document);
            List<GpuServiceClient.RerankResult> results = gpuServiceClient.rerank(query, documents, 1);

            if (results != null && !results.isEmpty()) {
                return results.get(0).getScore();
            }
        } catch (Exception e) {
            log.warn("Score computation failed: {}", e.getMessage());
        }

        return 0.0;
    }

    /**
     * 检查 Reranker 服务是否可用
     */
    public boolean isAvailable() {
        return gpuServiceClient.isHealthy();
    }
}
