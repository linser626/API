package com.airelay.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
