package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.messaging.NotificationService;
import com.lvatong.lft.model.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "系统通知、已读状态管理")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public ApiResult<Page<Notification>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(notificationService.getUserNotifications(userId, isRead, page, size));
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知")
    public ApiResult<List<Notification>> getUnreadNotifications() {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ApiResult<Map<String, Long>> getUnreadCount() {
        Long userId = 1L; // 临时硬编码
        long count = notificationService.getUnreadCount(userId);
        return ApiResult.success(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    public ApiResult<Void> markAsRead(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        notificationService.markAsRead(id, userId);
        return ApiResult.success(null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "标记所有通知已读")
    public ApiResult<Void> markAllAsRead() {
        Long userId = 1L; // 临时硬编码
        notificationService.markAllAsRead(userId);
        return ApiResult.success(null);
    }
}