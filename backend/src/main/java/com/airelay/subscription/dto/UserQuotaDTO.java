package com.airelay.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaDTO {

    private long tokenQuota;

    private long usedQuota;

    private int rateLimitRpm;

    private int rateLimitTpm;

    private int maxApiKeys;

    private String subscriptionStatus;
}
