package com.airelay.payment.dto;

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
public class OrderVO {

    private Long id;

    private String orderNo;

    private String type;

    private BigDecimal amount;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private String paymentMethod;

    private String paymentStatus;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
