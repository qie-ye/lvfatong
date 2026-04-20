package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "法律知识库", description = "法条查询、常见问题")
public class KnowledgeController {

    // TODO: v0.3 实现知识库API
    // - GET  /api/knowledge/laws (法条查询)
    // - GET  /api/knowledge/faq (常见问题)
    // - POST /api/knowledge/import (知识导入-管理员)

    @GetMapping("/laws")
    @Operation(summary = "法律条文查询")
    public ApiResult<Void> searchLaws() {
        return ApiResult.success();
    }

    @GetMapping("/faq")
    @Operation(summary = "常见问题")
    public ApiResult<Void> getFaq() {
        return ApiResult.success();
    }
}
