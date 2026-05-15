package com.airelay.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamCreateRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 100, message = "团队名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "团队描述长度不能超过500")
    private String description;

    private Integer maxMembers;
}
