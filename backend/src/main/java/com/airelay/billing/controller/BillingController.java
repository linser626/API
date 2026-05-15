package com.airelay.billing.controller;

import com.airelay.billing.dto.BalanceOverview;
import com.airelay.billing.dto.RechargeRequest;
import com.airelay.billing.dto.TransactionQuery;
import com.airelay.billing.entity.BalanceTransaction;
import com.airelay.billing.service.BillingService;
import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.payment.entity.Order;
import com.airelay.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "账单管理")
@RestController
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "获取余额概览")
    @GetMapping("/api/billing/overview")
    public Result<BalanceOverview> getBalanceOverview() {
        return Result.ok(billingService.getBalanceOverview(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "获取交易记录")
    @GetMapping("/api/billing/transactions")
    public Result<IPage<BalanceTransaction>> getTransactionList(TransactionQuery query) {
        if (query.getSize() > Constants.MAX_PAGE_SIZE) {
            query.setSize(Constants.MAX_PAGE_SIZE);
        }
        return Result.ok(billingService.getTransactionList(SecurityUtils.getCurrentUserId(), query));
    }

    @Operation(summary = "创建充值订单")
    @PostMapping("/api/billing/recharge")
    public Result<Order> recharge(@Valid @RequestBody RechargeRequest request) {
        return Result.ok(billingService.recharge(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "赠送余额(管理员)")
    @PostMapping("/api/admin/billing/gift")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BalanceTransaction> giftBalance(
            @RequestParam Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam String description) {
        return Result.ok(billingService.giftBalance(userId, amount, description));
    }
}
