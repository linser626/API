package com.airelay.user.dto;

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
public class ReferralRecordVO {

    private String referredUsername;
    private BigDecimal orderAmount;
    private BigDecimal commission;
    private String status;
    private LocalDateTime createdAt;
}
