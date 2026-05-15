package com.airelay.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedeemCouponRequest {

    @NotBlank(message = "优惠券代码不能为空")
    private String couponCode;
}
