package com.airelay.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("referral")
public class Referral implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String referralCode;

    private Long referredBy;

    private Integer totalReferrals;

    private BigDecimal totalEarned;

    private BigDecimal commissionRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
