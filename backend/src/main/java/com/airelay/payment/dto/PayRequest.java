package com.airelay.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
}
