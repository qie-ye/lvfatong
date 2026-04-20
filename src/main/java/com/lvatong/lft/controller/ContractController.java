package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
@Tag(name = "合同分析", description = "合同上传、条款分析、风险评估")
public class ContractController {

    // TODO: v0.3 实现合同分析API
    // - POST /api/contract/upload (文件上传)
    // - GET  /api/contract/{id}/analysis (获取分析结果)
    // - GET  /api/contract/list (合同列表)

    @PostMapping("/upload")
    @Operation(summary = "上传合同文档")
    public ApiResult<Void> upload() {
        return ApiResult.success();
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "获取合同分析结果")
    public ApiResult<Void> getAnalysis(@PathVariable Long id) {
        return ApiResult.success();
    }
}
