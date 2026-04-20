package com.lvatong.lft.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    // TODO: v0.2 实现智谱API HTTP客户端调用
    // - 非流式调用
    // - SSE流式调用
    // - 指数退避重试
    // - 模型降级
}
