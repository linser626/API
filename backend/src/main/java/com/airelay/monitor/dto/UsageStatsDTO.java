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
public class UsageStatsDTO {

    private long totalRequests;

    private long successRequests;

    private long failedRequests;

    private long totalTokens;

    private long promptTokens;

    private long completionTokens;

    private BigDecimal totalCost;

    private double avgLatencyMs;
}
