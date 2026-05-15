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
public class ChannelStatsDTO {

    private long channelId;

    private String channelName;

    private String channelType;

    private long totalRequests;

    private long failedRequests;

    private BigDecimal successRate;

    private double avgLatencyMs;
}
