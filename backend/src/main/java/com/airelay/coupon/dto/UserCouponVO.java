package com.airelay.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponVO {

    private Long id;

    private Long couponId;

    private String code;

    private String name;

    private String type;

    private BigDecimal value;

    private BigDecimal minAmount;

    private BigDecimal maxDiscount;

    private String status;

    private LocalDateTime usedAt;
}
