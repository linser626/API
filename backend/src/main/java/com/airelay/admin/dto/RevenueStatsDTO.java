package com.airelay.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsDTO {

    private String date;

    private BigDecimal revenue;

    private long orderCount;

    private BigDecimal rechargeAmount;

    private BigDecimal subscriptionAmount;
}
