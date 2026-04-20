package com.lvatong.lft.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    // TODO: v0.2 实现BGE-M3 embedding调用
    // - 批量embedding生成
    // - Redis缓存已生成的embedding
}
