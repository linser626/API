package com.airelay.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionVO {

    private Long id;

    private Long planId;

    private String planName;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer autoRenew;

    private Long tokenQuota;

    private Long usedQuota;

    private Integer rateLimitRpm;

    private Integer rateLimitTpm;

    private Integer maxApiKeys;
}
