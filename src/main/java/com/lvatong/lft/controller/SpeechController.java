package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.service.XfyunAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@Tag(name = "语音识别", description = "科大讯飞语音识别鉴权")
public class SpeechController {

    private final XfyunAuthService xfyunAuthService;

    @GetMapping("/auth")
    @Operation(summary = "获取语音识别鉴权URL", description = "返回带HMAC签名的讯飞IAT WebSocket URL，5分钟有效")
    public ApiResult<XfyunAuthService.AuthResult> getAuthUrl(
            @RequestParam(defaultValue = "mandarin") String dialect) {
        return ApiResult.success(xfyunAuthService.generateAuthUrl(dialect));
    }
}
