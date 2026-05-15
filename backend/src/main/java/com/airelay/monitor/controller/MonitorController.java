package com.airelay.monitor.controller;

import com.airelay.common.Result;
import com.airelay.monitor.dto.ChannelStatsDTO;
import com.airelay.monitor.dto.DailyUsageDTO;
import com.airelay.monitor.dto.DashboardStatsDTO;
import com.airelay.monitor.dto.ModelUsageDTO;
import com.airelay.monitor.dto.UsageStatsDTO;
import com.airelay.monitor.service.MonitorService;
import com.airelay.relay.entity.RequestLog;
import com.airelay.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "监控统计")
@RestController
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "获取我的使用统计")
    @GetMapping("/api/monitor/usage")
    public Result<UsageStatsDTO> getUserUsageStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(monitorService.getUserUsageStats(userId, startTime, endTime));
    }

    @Operation(summary = "获取我的模型使用分布")
    @GetMapping("/api/monitor/usage/models")
    public Result<List<ModelUsageDTO>> getUserModelUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(monitorService.getUserModelUsage(userId, startTime, endTime));
    }

    @Operation(summary = "获取我的每日使用趋势")
    @GetMapping("/api/monitor/usage/daily")
    public Result<List<DailyUsageDTO>> getUserDailyUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(monitorService.getUserDailyUsage(userId, startTime, endTime));
    }

    @Operation(summary = "管理员仪表盘统计")
    @GetMapping("/api/admin/monitor/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsDTO> getDashboardStats() {
        return Result.ok(monitorService.getDashboardStats());
    }

    @Operation(summary = "通道性能统计")
    @GetMapping("/api/admin/monitor/channels")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ChannelStatsDTO>> getChannelStats() {
        return Result.ok(monitorService.getChannelStats());
    }

    @Operation(summary = "获取最近错误请求")
    @GetMapping("/api/admin/monitor/errors")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RequestLog>> getRecentErrors(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(monitorService.getRecentErrors(limit));
    }
}
