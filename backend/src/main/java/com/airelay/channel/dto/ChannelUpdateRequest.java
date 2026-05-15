package com.airelay.channel.dto;

import lombok.Data;

@Data
public class ChannelUpdateRequest {

    private String name;

    private String baseUrl;

    private String apiKey;

    private String models;

    private Integer priority;

    private Integer weight;

    private Integer status;

    private Integer maxRetries;

    private Integer timeoutMs;
}
