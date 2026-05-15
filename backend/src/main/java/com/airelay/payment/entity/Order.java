package com.airelay.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String type;

    private BigDecimal amount;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private String paymentMethod;

    private String paymentStatus;

    private String paymentNo;

    private LocalDateTime paidAt;

    private Long couponId;

    private Long planId;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
