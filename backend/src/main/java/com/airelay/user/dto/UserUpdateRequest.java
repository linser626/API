package com.airelay.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatar;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;
}
