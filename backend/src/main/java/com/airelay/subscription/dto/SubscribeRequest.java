package com.airelay.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long planId;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    private String couponCode;
}
