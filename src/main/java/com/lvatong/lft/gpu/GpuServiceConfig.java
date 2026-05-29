package com.lvatong.lft.gpu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "lvatong.gpu")
public class GpuServiceConfig {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:8081";
    private int timeout = 5000;
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long backoff = 1000;
    }
}
