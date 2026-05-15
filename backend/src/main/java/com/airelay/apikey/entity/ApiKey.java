package com.airelay.apikey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("api_key")
public class ApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String keyValue;

    private String name;

    private Integer status;

    private Integer rateLimitRpm;

    private Integer rateLimitTpm;

    private Long totalQuota;

    private Long usedQuota;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
