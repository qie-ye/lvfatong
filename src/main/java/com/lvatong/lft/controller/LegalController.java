package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.ChatRequest;
import com.lvatong.lft.model.dto.SearchRequest;
import com.lvatong.lft.model.entity.ChatMessage;
import com.lvatong.lft.model.entity.ChatSession;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.service.ChatMemoryService;
import com.lvatong.lft.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@Tag(name = "法律咨询", description = "智能法律问答、法条检索、多轮对话")
public class LegalController {

    private final LegalService legalService;
    private final HybridSearchService hybridSearchService;
    private final ChatMemoryService chatMemoryService;

    @PostMapping("/chat")
    @RateLimit(permitsPerSecond = 3.0, dimension = "USER", message = "咨询请求过于频繁，请稍后再试")
    @Operation(summary = "法律问答（SSE流式）", description = "发送法律问题，以SSE流式返回AI回答")
    public SseEmitter chat(@RequestBody ChatRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return legalService.chatStream(userId, request.getSessionId(), request.getQuestion(),
                request.getDocType(), request.getLawDomain());
    }

    @PostMapping(value = "/chat/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "法律问答（非流式）")
    public ApiResult<ChatMessage> chatSync(@RequestBody ChatRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ChatMessage message = legalService.chat(userId, request.getSessionId(), request.getQuestion(),
                request.getDocType(), request.getLawDomain());
        return ApiResult.success(message);
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建对话会话")
    public ApiResult<ChatSession> createSession(@RequestParam(name = "title", required = false) String title, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalService.createSession(userId, title));
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取对话列表")
    public ApiResult<List<ChatSession>> getSessions(Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            return ApiResult.success(legalService.getSessions(userId));
        } catch (Exception e) {
            log.error("Failed to load sessions: {}", e.getMessage(), e);
            return ApiResult.success(List.of());
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取对话消息")
    public ApiResult<List<ChatMessage>> getMessages(@PathVariable("sessionId") Long sessionId) {
        return ApiResult.success(legalService.getMessages(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除对话会话")
    public ApiResult<String> deleteSession(@PathVariable("sessionId") Long sessionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        legalService.deleteSession(userId, sessionId);
        return ApiResult.success("会话已删除");
    }

    @PutMapping("/sessions/{sessionId}")
    @Operation(summary = "重命名对话会话")
    public ApiResult<ChatSession> renameSession(@PathVariable("sessionId") Long sessionId,
                                                 @RequestParam("title") String title,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalService.renameSession(userId, sessionId, title));
    }

    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "结束对话会话", description = "显式结束会话，触发摘要生成和记忆更新")
    public ApiResult<String> endSession(@PathVariable("sessionId") Long sessionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        legalService.endSession(userId, sessionId);
        return ApiResult.success("会话已结束");
    }

    @PostMapping("/sessions/new")
    @Operation(summary = "开始新会话", description = "结束当前会话并创建新会话，自动预热记忆")
    public ApiResult<ChatSession> startNewSession(
            @RequestParam(name = "currentSessionId", required = false) Long currentSessionId,
            @RequestParam(name = "question", required = false) String question,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(legalService.startNewSession(userId, currentSessionId, question));
    }

    @GetMapping("/sessions/{sessionId}/context-usage")
    @Operation(summary = "查询上下文用量", description = "返回当前会话的上下文 token 用量和是否需要压缩")
    public ApiResult<ChatMemoryService.ContextUsage> contextUsage(@PathVariable("sessionId") Long sessionId) {
        return ApiResult.success(chatMemoryService.estimateContextUsage(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/compress")
    @Operation(summary = "压缩上下文", description = "将旧消息摘要压缩，释放上下文空间")
    public ApiResult<String> compressContext(@PathVariable("sessionId") Long sessionId) {
        boolean ok = chatMemoryService.compressContext(sessionId);
        return ok ? ApiResult.success("上下文已压缩") : ApiResult.error(400, "无需压缩或压缩失败");
    }

    @PostMapping("/search")
    @Operation(summary = "法条检索")
    public ApiResult<List<HybridSearchService.SearchResult>> searchLaw(@RequestBody SearchRequest request) {
        List<HybridSearchService.SearchResult> results = hybridSearchService.search(
                request.getQuery(), request.getDocType(), request.getLawDomain(), request.getTopK());
        return ApiResult.success(results);
    }
}
