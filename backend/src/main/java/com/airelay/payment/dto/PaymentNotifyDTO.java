package com.airelay.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyDTO {

    private String orderNo;

    private String paymentNo;

    private String paymentMethod;

    private BigDecimal amount;

    private String status;

    private String tradeNo;
}
