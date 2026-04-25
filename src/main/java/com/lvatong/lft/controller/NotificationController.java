package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.Notification;
import com.lvatong.lft.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "未读通知查询与已读标记")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取当前用户所有通知")
    public ApiResult<List<Notification>> getAll(Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            return ApiResult.success(notificationService.getAll(userId));
        } catch (Exception e) {
            log.error("Failed to load notifications: {}", e.getMessage(), e);
            return ApiResult.success(List.of());
        }
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public ApiResult<Long> unreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(notificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条通知为已读")
    public ApiResult<Void> markRead(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markRead(userId, id);
        return ApiResult.success(null);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标为已读")
    public ApiResult<Void> markAllRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllRead(userId);
        return ApiResult.success(null);
    }
}
