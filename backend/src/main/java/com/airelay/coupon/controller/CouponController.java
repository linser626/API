package com.airelay.coupon.controller;

import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.coupon.dto.CouponCreateRequest;
import com.airelay.coupon.dto.CouponVO;
import com.airelay.coupon.dto.RedeemCouponRequest;
import com.airelay.coupon.dto.UserCouponVO;
import com.airelay.coupon.service.CouponService;
import com.airelay.payment.entity.Coupon;
import com.airelay.payment.entity.UserCoupon;
import com.airelay.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "优惠券管理")
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "创建优惠券")
    @PostMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Coupon> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return Result.ok(couponService.createCoupon(request));
    }

    @Operation(summary = "更新优惠券")
    @PutMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponCreateRequest request) {
        return Result.ok(couponService.updateCoupon(id, request));
    }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.ok();
    }

    @Operation(summary = "获取优惠券列表")
    @GetMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<CouponVO>> listCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) Integer status) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(couponService.listCoupons(page, size, status));
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/api/coupon/redeem")
    public Result<UserCoupon> redeemCoupon(@Valid @RequestBody RedeemCouponRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(couponService.redeemCoupon(userId, request.getCouponCode()));
    }

    @Operation(summary = "获取我的优惠券")
    @GetMapping("/api/coupon/my")
    public Result<List<UserCouponVO>> getMyCoupons(@RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(couponService.getUserCoupons(userId, status));
    }
}
