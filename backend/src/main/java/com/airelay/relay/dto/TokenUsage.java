package com.airelay.relay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
