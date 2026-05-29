package com.lvatong.lft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lvatong.multimodal.ocr")
public class OcrConfig {
    private boolean enabled = true;
    private String provider = "baidu";
    private String apiKey;
    private String secretKey;
    private int timeout = 5000;
}