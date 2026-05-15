package com.airelay.subscription.controller;

import com.airelay.common.Result;
import com.airelay.payment.entity.Order;
import com.airelay.security.SecurityUtils;
import com.airelay.subscription.dto.PlanVO;
import com.airelay.subscription.dto.SubscribeRequest;
import com.airelay.subscription.dto.SubscriptionVO;
import com.airelay.subscription.dto.UserQuotaDTO;
import com.airelay.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订阅管理")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "获取套餐列表")
    @GetMapping("/api/subscription/plans")
    public Result<List<PlanVO>> listPlans() {
        return Result.ok(subscriptionService.listPlans());
    }

    @Operation(summary = "获取当前订阅")
    @GetMapping("/api/subscription/current")
    public Result<SubscriptionVO> getCurrentSubscription() {
        return Result.ok(subscriptionService.getCurrentSubscription(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "订阅套餐")
    @PostMapping("/api/subscription/subscribe")
    public Result<Order> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return Result.ok(subscriptionService.subscribe(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "取消自动续费")
    @PutMapping("/api/subscription/cancel")
    public Result<Void> cancelSubscription(@RequestParam Long subscriptionId) {
        subscriptionService.cancelSubscription(SecurityUtils.getCurrentUserId(), subscriptionId);
        return Result.ok();
    }

    @Operation(summary = "获取配额和限制")
    @GetMapping("/api/subscription/quota")
    public Result<UserQuotaDTO> getUserQuotaAndLimits() {
        return Result.ok(subscriptionService.getUserQuotaAndLimits(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "启用自动续费")
    @PostMapping("/api/subscription/auto-renew/enable")
    public Result<Void> enableAutoRenew(@RequestBody Map<String, String> body) {
        String paymentMethod = body.get("paymentMethod");
        subscriptionService.enableAutoRenew(SecurityUtils.getCurrentUserId(), paymentMethod);
        return Result.ok();
    }

    @Operation(summary = "禁用自动续费")
    @PostMapping("/api/subscription/auto-renew/disable")
    public Result<Void> disableAutoRenew() {
        subscriptionService.disableAutoRenew(SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
