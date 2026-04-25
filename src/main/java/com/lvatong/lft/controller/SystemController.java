package com.lvatong.lft.controller;

import com.lvatong.lft.ai.ZhipuApiClient;
import com.lvatong.lft.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ZhipuApiClient zhipuApiClient;

    @GetMapping("/ai-status")
    public ApiResult<Map<String, Object>> getAiStatus() {
        boolean configured = zhipuApiClient.isApiKeyConfigured();
        return ApiResult.success(Map.of(
                "available", configured,
                "message", configured ? "AI服务已配置" : "AI服务未配置（ZHIPU_API_KEY未设置），法律意见书等功能不可用"
        ));
    }
}
