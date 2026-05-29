package com.lvatong.lft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lvatong.multimodal.tts")
public class TtsConfig {
    private boolean enabled = true;
    private String provider = "xfyun";
    private String voice = "xiaoyan";
    private String apiKey;
    private String apiSecret;
    private int timeout = 5000;
}