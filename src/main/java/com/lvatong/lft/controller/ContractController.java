package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.contract.ContractService;
import com.lvatong.lft.model.dto.ContractAnalysisResult;
import com.lvatong.lft.model.dto.ContractModificationSuggestion;
import com.lvatong.lft.model.dto.ContractUploadResponse;
import com.lvatong.lft.model.entity.ContractDocument;
import com.lvatong.lft.model.entity.ContractTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/contract")
@RequiredArgsConstructor
@Tag(name = "合同分析", description = "合同上传、条款分析、风险评估")
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/upload")
    @Operation(summary = "上传合同文档（支持PDF/Word）")
    public ApiResult<ContractUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(contractService.upload(userId, file));
    }

    @PostMapping("/{id}/analyze")
    @RateLimit(permitsPerSecond = 2.0, dimension = "USER", message = "合同分析请求过于频繁，请稍后再试")
    @Operation(summary = "触发合同分析（异步）")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResult<Map<String, Object>> analyze(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        contractService.analyzeAsync(userId, id);
        return ApiResult.success(Map.of("contractId", id, "status", "ANALYZING"));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "查询合同分析状态")
    public ApiResult<Map<String, Object>> status(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ContractDocument doc = contractService.getContractForUser(userId, id);
        return ApiResult.success(Map.of("contractId", id, "status", doc.getStatus().name()));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "获取合同分析结果")
    public ApiResult<ContractAnalysisResult> getAnalysis(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(contractService.getAnalysisResult(userId, id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户合同列表")
    public ApiResult<List<ContractDocument>> list(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(contractService.listByUser(userId));
    }

    @GetMapping("/{id}/suggestions")
    @Operation(summary = "获取合同修改建议")
    public ApiResult<List<ContractModificationSuggestion>> getSuggestions(
            @PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(contractService.generateModificationSuggestions(userId, id));
    }

    @PostMapping("/compare")
    @Operation(summary = "合同对比分析")
    public ApiResult<String> compareContracts(
            @RequestParam("contractId1") Long contractId1,
            @RequestParam("contractId2") Long contractId2,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(contractService.compareContracts(userId, contractId1, contractId2));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取合同模板列表")
    public ApiResult<List<ContractTemplate>> listTemplates(
            @RequestParam(name = "category", required = false) String category) {
        return ApiResult.success(contractService.listTemplates(category));
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "获取合同模板详情")
    public ApiResult<ContractTemplate> getTemplate(@PathVariable("id") Long id) {
        return ApiResult.success(contractService.getTemplate(id));
    }
}
