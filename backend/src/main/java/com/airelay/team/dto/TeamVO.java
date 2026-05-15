package com.airelay.team.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TeamVO {

    private Long id;
    private String name;
    private Long ownerId;
    private String ownerName;
    private String description;
    private String avatar;
    private BigDecimal balance;
    private Integer memberCount;
    private Integer maxMembers;
    private String myRole;
    private Integer status;
    private LocalDateTime createdAt;
}
