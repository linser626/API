package com.airelay.team.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeamMemberVO {

    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String nickname;
    private Integer status;
    private LocalDateTime joinedAt;
}
