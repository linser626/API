package com.airelay.team.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("team_api_key")
public class TeamApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private String keyValue;

    private String name;

    private Long createdBy;

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
