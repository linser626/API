package com.airelay.apikey.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyUpdateRequest {

    @Size(max = 100, message = "密钥名称长度不能超过100")
    private String name;

    private Integer rateLimitRpm;

    private Integer rateLimitTpm;
}
