package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
@Tag(name = "法律咨询", description = "智能法律问答、法条检索、多轮对话")
public class LegalController {

    // TODO: v0.2 实现法律问答API
    // - POST /api/legal/chat (SSE流式)
    // - GET  /api/legal/sessions
    // - GET  /api/legal/sessions/{id}/messages
    // - POST /api/legal/search (法条检索)

    @PostMapping("/chat")
    @Operation(summary = "法律问答（SSE流式）")
    public ApiResult<Void> chat() {
        return ApiResult.success();
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取对话列表")
    public ApiResult<Void> getSessions() {
        return ApiResult.success();
    }

    @PostMapping("/search")
    @Operation(summary = "法条检索")
    public ApiResult<Void> searchLaw() {
        return ApiResult.success();
    }
}
