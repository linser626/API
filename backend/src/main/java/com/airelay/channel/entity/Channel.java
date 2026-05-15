package com.airelay.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("channel")
public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String baseUrl;

    private String apiKey;

    private String models;

    private Integer priority;

    private Integer weight;

    private Integer status;

    private Integer maxRetries;

    private Integer timeoutMs;

    private Integer responseTimeMs;

    private BigDecimal successRate;

    private Long totalRequests;

    private Long failedRequests;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
