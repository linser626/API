package com.airelay.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    private String role = "member";
}
