package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.multimodal.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR服务", description = "图片文字识别")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/recognize")
    @Operation(summary = "图片OCR识别")
    public ApiResult<Map<String, Object>> recognizeText(@RequestParam("file") MultipartFile file) {
        try {
            return ApiResult.success(ocrService.recognizeText(file));
        } catch (IOException e) {
            return ApiResult.error("OCR识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/contract")
    @Operation(summary = "合同扫描件识别")
    public ApiResult<Map<String, Object>> recognizeContract(@RequestParam("file") MultipartFile file) {
        try {
            return ApiResult.success(ocrService.recognizeContract(file));
        } catch (IOException e) {
            return ApiResult.error("合同识别失败: " + e.getMessage());
        }
    }
}