package com.airelay.team.dto;

import lombok.Data;

@Data
public class TeamUpdateRequest {

    private String name;
    private String description;
    private String avatar;
    private Integer maxMembers;
}
