package com.lvatong.lft.gpu;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lvatong.gpu.enabled", havingValue = "true", matchIfMissing = true)
public class GpuServiceClient {

    private final GpuServiceConfig config;

    private RestClient restClient;

    private RestClient getClient() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(config.getBaseUrl())
                    .build();
        }
        return restClient;
    }

    /**
     * Rerank documents based on query relevance
     */
    public List<RerankResult> rerank(String query, List<String> documents, int topK) {
        if (!config.isEnabled()) {
            log.debug("GPU service disabled, returning original order");
            return getDefaultRerankResults(documents, topK);
        }

        try {
            RerankRequest request = new RerankRequest();
            request.setQuery(query);
            request.setDocuments(documents);
            request.setTopK(topK);

            RerankResponse response = getClient()
                    .post()
                    .uri("/rerank")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("GPU rerank client error: {}", res.getStatusCode());
                        throw new RuntimeException("GPU rerank request failed");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("GPU rerank server error: {}", res.getStatusCode());
                        throw new RuntimeException("GPU service unavailable");
                    })
                    .body(RerankResponse.class);

            return response != null ? response.getResults() : getDefaultRerankResults(documents, topK);

        } catch (Exception e) {
            log.warn("GPU rerank failed, falling back to default order: {}", e.getMessage());
            return getDefaultRerankResults(documents, topK);
        }
    }

    /**
     * Generate embeddings for texts
     */
    public List<List<Float>> embed(List<String> texts) {
        if (!config.isEnabled()) {
            log.debug("GPU service disabled, returning empty embeddings");
            return new ArrayList<>();
        }

        try {
            EmbedRequest request = new EmbedRequest();
            request.setTexts(texts);

            EmbedResponse response = getClient()
                    .post()
                    .uri("/embed")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("GPU embed client error: {}", res.getStatusCode());
                        throw new RuntimeException("GPU embed request failed");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("GPU embed server error: {}", res.getStatusCode());
                        throw new RuntimeException("GPU service unavailable");
                    })
                    .body(EmbedResponse.class);

            return response != null ? response.getEmbeddings() : new ArrayList<>();

        } catch (Exception e) {
            log.warn("GPU embed failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Generate embedding for a single query
     */
    public List<Float> embedQuery(String query) {
        List<List<Float>> results = embed(List.of(query));
        return results.isEmpty() ? new ArrayList<>() : results.get(0);
    }

    /**
     * Check GPU service health
     */
    public boolean isHealthy() {
        if (!config.isEnabled()) {
            return false;
        }

        try {
            HealthResponse response = getClient()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .body(HealthResponse.class);

            return response != null && "ok".equals(response.getStatus());

        } catch (Exception e) {
            log.debug("GPU health check failed: {}", e.getMessage());
            return false;
        }
    }

    private List<RerankResult> getDefaultRerankResults(List<String> documents, int topK) {
        List<RerankResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(documents.size(), topK); i++) {
            RerankResult result = new RerankResult();
            result.setIndex(i);
            result.setScore(1.0 - (i * 0.1)); // Descending score
            result.setDocument(documents.get(i));
            results.add(result);
        }
        return results;
    }

    // DTOs
    @Data
    public static class RerankRequest {
        private String query;
        private List<String> documents;
        private int topK;
    }

    @Data
    public static class RerankResponse {
        private List<RerankResult> results;
        private String model;
    }

    @Data
    public static class RerankResult {
        private int index;
        private double score;
        private String document;
    }

    @Data
    public static class EmbedRequest {
        private List<String> texts;
    }

    @Data
    public static class EmbedResponse {
        private List<List<Float>> embeddings;
        private String model;
        private int dimension;
    }

    @Data
    public static class HealthResponse {
        private String status;
        private boolean rerankerReady;
        private boolean embeddingReady;
    }
}
