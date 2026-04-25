package com.lvatong.lft.config;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
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
    private String token = "";

    @Bean
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
    public MilvusClientV2 milvusClient() {
        ConnectConfig.ConnectConfigBuilder<?, ?> builder = ConnectConfig.builder()
            .uri(String.format("http://%s:%d", host, port))
            .dbName(dbName);
        if (token != null && !token.isBlank()) {
            builder.token(token);
        }
        ConnectConfig config = builder.build();
        MilvusClientV2 client = new MilvusClientV2(config);
        log.info("Milvus client connected to {}:{} (auth={})", host, port, token != null && !token.isBlank());
        return client;
    }
}
