package com.airelay.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelCreateRequest {

    @NotBlank(message = "通道名称不能为空")
    private String name;

    @NotBlank(message = "通道类型不能为空")
    private String type;

    @NotBlank(message = "基础URL不能为空")
    private String baseUrl;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    @NotBlank(message = "模型列表不能为空")
    private String models;

    private Integer priority;

    private Integer weight;

    private Integer maxRetries;

    private Integer timeoutMs;
}
