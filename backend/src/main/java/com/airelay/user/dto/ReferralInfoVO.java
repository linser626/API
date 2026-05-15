package com.airelay.user.dto;

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
public class ReferralInfoVO {

    private String referralCode;
    private String referralLink;
    private Integer totalReferrals;
    private BigDecimal totalEarned;
    private BigDecimal commissionRate;
    private List<ReferralRecordVO> recentRecords;
}
