package com.airelay.team.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeamApiKeyVO {

    private Long id;
    private String name;
    private String keyValue;
    private Integer status;
    private Integer rateLimitRpm;
    private Integer rateLimitTpm;
    private Long totalQuota;
    private Long usedQuota;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
