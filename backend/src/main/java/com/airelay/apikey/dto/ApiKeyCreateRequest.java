package com.airelay.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyCreateRequest {

    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 100, message = "密钥名称长度不能超过100")
    private String name;
}
