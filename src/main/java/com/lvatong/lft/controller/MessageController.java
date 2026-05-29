package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.messaging.MessageService;
import com.lvatong.lft.model.entity.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "消息管理", description = "团队消息、私信发送和接收")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/team/{teamId}")
    @Operation(summary = "发送团队消息")
    public ApiResult<Message> sendTeamMessage(@PathVariable Long teamId, @RequestBody SendMessageRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(messageService.sendTeamMessage(teamId, userId, request.getContent()));
    }

    @PostMapping("/private/{receiverId}")
    @Operation(summary = "发送私信")
    public ApiResult<Message> sendPrivateMessage(@PathVariable Long receiverId, @RequestBody SendMessageRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(messageService.sendPrivateMessage(userId, receiverId, request.getContent()));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "获取团队消息列表")
    public ApiResult<Page<Message>> getTeamMessages(@PathVariable Long teamId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResult.success(messageService.getTeamMessages(teamId, page, size));
    }

    @GetMapping("/channel/{channelType}/{channelId}")
    @Operation(summary = "获取频道消息列表")
    public ApiResult<Page<Message>> getChannelMessages(@PathVariable Message.ChannelType channelType,
                                                        @PathVariable Long channelId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResult.success(messageService.getChannelMessages(channelType, channelId, page, size));
    }

    @GetMapping("/private")
    @Operation(summary = "获取私信列表")
    public ApiResult<Page<Message>> getPrivateMessages(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(messageService.getPrivateMessages(userId, page, size));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记消息已读")
    public ApiResult<Void> markAsRead(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        messageService.markAsRead(id, userId);
        return ApiResult.success(null);
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }
}