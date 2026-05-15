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
public class CouponVO {

    private Long id;

    private String code;

    private String name;

    private String type;

    private BigDecimal value;

    private BigDecimal minAmount;

    private BigDecimal maxDiscount;

    private Integer totalCount;

    private Integer usedCount;

    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
