package com.airelay.monitor.controller;

import com.airelay.common.Result;
import com.airelay.monitor.service.NotificationService;
import com.airelay.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通知管理")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取我的通知列表")
    @GetMapping("/api/notifications")
    public Result<List<Map<String, Object>>> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(notificationService.getUserNotifications(userId));
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/api/notifications/{id}/read")
    public Result<Void> markAsRead(@PathVariable String id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return Result.ok();
    }

    @Operation(summary = "标记所有通知为已读")
    @PutMapping("/api/notifications/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
