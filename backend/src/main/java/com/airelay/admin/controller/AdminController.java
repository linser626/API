package com.airelay.admin.controller;

import com.airelay.admin.dto.ModelPriceCreateRequest;
import com.airelay.admin.dto.RevenueStatsDTO;
import com.airelay.admin.dto.UserStatsDTO;
import com.airelay.admin.service.AdminService;
import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.monitor.dto.DailyUsageDTO;
import com.airelay.monitor.dto.DashboardStatsDTO;
import com.airelay.monitor.service.MonitorService;
import com.airelay.relay.entity.ModelPrice;
import com.airelay.relay.service.ContentModerationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MonitorService monitorService;
    private final ContentModerationService contentModerationService;

    @Operation(summary = "平台概览")
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsDTO> getOverview() {
        return Result.ok(monitorService.getDashboardStats());
    }

    @Operation(summary = "收入统计")
    @GetMapping("/stats/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RevenueStatsDTO>> getRevenueStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.ok(adminService.getRevenueStats(startTime, endTime));
    }

    @Operation(summary = "用户增长统计")
    @GetMapping("/stats/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserStatsDTO>> getUserStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.ok(adminService.getUserStats(startTime, endTime));
    }

    @Operation(summary = "请求统计")
    @GetMapping("/stats/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<DailyUsageDTO>> getRequestStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.ok(adminService.getRequestStats(startTime, endTime));
    }

    @Operation(summary = "添加模型定价")
    @PostMapping("/model-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ModelPrice> addModelPrice(@Valid @RequestBody ModelPriceCreateRequest request) {
        return Result.ok(adminService.addModelPrice(request));
    }

    @Operation(summary = "获取模型定价列表")
    @GetMapping("/model-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<ModelPrice>> listModelPrices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(adminService.listModelPrices(page, size));
    }

    @Operation(summary = "删除模型定价")
    @DeleteMapping("/model-prices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteModelPrice(@PathVariable Long id) {
        adminService.deleteModelPrice(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用通道")
    @PostMapping("/channels/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleChannel(@PathVariable Long id, @RequestParam boolean enabled) {
        adminService.toggleChannel(id, enabled);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetUserPassword(@PathVariable Long id, @RequestParam String newPassword) {
        adminService.resetUserPassword(id, newPassword);
        return Result.ok();
    }

    @Operation(summary = "获取内容审核状态")
    @GetMapping("/moderation/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> getModerationStatus() {
        return Result.ok(contentModerationService.isEnabled());
    }

    @Operation(summary = "切换内容审核开关")
    @PostMapping("/moderation/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleModeration(@RequestParam boolean enabled) {
        contentModerationService.setEnabled(enabled);
        return Result.ok();
    }
}
