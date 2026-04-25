package com.lvatong.lft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "xfyun")
public class XfyunConfig {

    private String appId;
    private String apiKey;
    private String apiSecret;
    private String iatUrl = "wss://iat-api.xfyun.cn/v2/iat";
    private int authTtl = 300;
}
