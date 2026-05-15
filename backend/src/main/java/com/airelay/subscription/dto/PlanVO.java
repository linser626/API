package com.airelay.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanVO {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer durationDays;

    private Long tokenQuota;

    private Integer rateLimitRpm;

    private Integer rateLimitTpm;

    private Integer maxApiKeys;

    private List<String> features;

    private Integer isDefault;

    private Integer sortOrder;

    private Integer status;
}
