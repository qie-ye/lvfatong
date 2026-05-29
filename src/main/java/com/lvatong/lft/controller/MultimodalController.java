package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.multimodal.AsrService;
import com.lvatong.lft.multimodal.MultimodalChatService;
import com.lvatong.lft.multimodal.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/multimodal")
@RequiredArgsConstructor
@Tag(name = "多模态服务", description = "语音、图片、OCR等多模态功能")
public class MultimodalController {

    private final MultimodalChatService multimodalChatService;
    private final TtsService ttsService;
    private final AsrService asrService;

    @PostMapping("/chat")
    @Operation(summary = "多模态聊天")
    public ApiResult<Map<String, Object>> multimodalChat(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio) {
        return ApiResult.success(multimodalChatService.multimodalChat(text, image, audio));
    }

    @PostMapping("/image-question")
    @Operation(summary = "图片问答")
    public ApiResult<Map<String, Object>> imageQuestion(
            @RequestParam("image") MultipartFile image,
            @RequestParam("question") String question) {
        return ApiResult.success(multimodalChatService.imageQuestion(image, question));
    }

    @PostMapping("/voice-question")
    @Operation(summary = "语音问答")
    public ApiResult<Map<String, Object>> voiceQuestion(@RequestParam("audio") MultipartFile audio) {
        return ApiResult.success(multimodalChatService.voiceQuestion(audio));
    }

    @PostMapping("/asr")
    @Operation(summary = "语音识别")
    public ApiResult<Map<String, Object>> recognizeSpeech(@RequestParam("audio") MultipartFile audio) {
        try {
            return ApiResult.success(asrService.recognizeSpeech(audio));
        } catch (IOException e) {
            return ApiResult.error("语音识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/tts")
    @Operation(summary = "文本转语音")
    public ApiResult<Map<String, Object>> textToSpeech(
            @RequestParam("text") String text,
            @RequestParam(value = "voice", required = false) String voice) {
        return ApiResult.success(ttsService.textToSpeech(text, voice));
    }

    @GetMapping("/voices")
    @Operation(summary = "获取可用语音列表")
    public ApiResult<Map<String, Object>> getAvailableVoices() {
        return ApiResult.success(ttsService.getAvailableVoices());
    }
}