package com.airelay.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCreateRequest {

    @NotBlank(message = "优惠券代码不能为空")
    @Size(max = 50, message = "优惠券代码长度不能超过50")
    private String code;

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotBlank(message = "优惠券类型不能为空")
    private String type;

    @NotNull(message = "优惠券面值不能为空")
    private BigDecimal value;

    private BigDecimal minAmount;

    private BigDecimal maxDiscount;

    private Integer totalCount = -1;

    private Integer perUserLimit = 1;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
