package com.lvatong.lft.config;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    private String host = "localhost";
    private int port = 19530;
    private String dbName = "default";
    private String collectionName = "legal_vectors";
    private int embeddingDim = 1024;
    private String indexType = "HNSW";
    private String metricType = "COSINE";
    private String indexParams = "{\"M\":16,\"efConstruction\":256}";
    private String searchParams = "{\"ef\":128}";

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig config = ConnectConfig.builder()
            .uri(String.format("http://%s:%d", host, port))
            .dbName(dbName)
            .build();
        MilvusClientV2 client = new MilvusClientV2(config);
        log.info("Milvus client connected to {}:{}", host, port);
        return client;
    }
}
