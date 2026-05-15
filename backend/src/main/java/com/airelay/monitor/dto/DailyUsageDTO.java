package com.airelay.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageDTO {

    private String date;

    private long requestCount;

    private long totalTokens;

    private BigDecimal totalCost;
}
