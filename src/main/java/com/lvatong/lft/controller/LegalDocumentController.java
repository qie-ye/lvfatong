package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.GenerateDocumentRequest;
import com.lvatong.lft.model.dto.LegalDocumentResponse;
import com.lvatong.lft.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "法律文书", description = "AI生成法律文书（起诉状、答辩状等）")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @PostMapping
    @RateLimit(permitsPerSecond = 1.0, dimension = "USER", message = "文书生成请求过于频繁，请稍后再试")
    @Operation(summary = "生成法律文书（异步，GLM-4-Plus）")
    public ApiResult<LegalDocumentResponse> generate(
            @Valid @RequestBody GenerateDocumentRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalDocumentService.generateDocument(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文书详情")
    public ApiResult<LegalDocumentResponse> getDocument(
            @PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalDocumentService.getDocument(userId, id));
    }

    @GetMapping
    @Operation(summary = "获取用户文书列表")
    public ApiResult<List<LegalDocumentResponse>> list(
            @RequestParam(name = "docType", required = false) String docType,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        if (docType != null && !docType.isBlank()) {
            return ApiResult.success(legalDocumentService.listByUserAndType(userId, docType));
        }
        return ApiResult.success(legalDocumentService.listByUser(userId));
    }
}
