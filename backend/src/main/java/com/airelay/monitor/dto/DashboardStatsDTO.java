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
public class DashboardStatsDTO {

    private long totalUsers;

    private long activeUsers;

    private long todayRequests;

    private long todayTokens;

    private BigDecimal todayRevenue;

    private BigDecimal totalRevenue;

    private long activeChannels;

    private long activeSubscriptions;
}
