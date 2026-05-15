package com.airelay.relay.service;

import com.airelay.relay.dto.TokenUsage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCountService {

    private final ObjectMapper objectMapper;

    private static final int AVERAGE_CHARS_PER_TOKEN = 4;

    public int countTokens(String model, String requestBody) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            int totalChars = 0;

            if (root.has("messages")) {
                JsonNode messages = root.get("messages");
                for (JsonNode message : messages) {
                    if (message.has("content")) {
                        String content = message.get("content").asText();
                        totalChars += content.length();
                    }
                }
            }

            if (root.has("prompt")) {
                totalChars += root.get("prompt").asText().length();
            }

            if (root.has("input")) {
                JsonNode input = root.get("input");
                if (input.isTextual()) {
                    totalChars += input.asText().length();
                } else if (input.isArray()) {
                    for (JsonNode item : input) {
                        totalChars += item.asText().length();
                    }
                }
            }

            return totalChars / AVERAGE_CHARS_PER_TOKEN;
        } catch (JsonProcessingException e) {
            log.warn("估算token数量失败: {}", e.getMessage());
            return 0;
        }
    }

    public TokenUsage extractTokenUsage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.get("usage");
            if (usage != null && !usage.isNull()) {
                int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : promptTokens + completionTokens;
                return new TokenUsage(promptTokens, completionTokens, totalTokens);
            }
            return new TokenUsage(0, 0, 0);
        } catch (JsonProcessingException e) {
            log.warn("解析token使用量失败: {}", e.getMessage());
            return new TokenUsage(0, 0, 0);
        }
    }

    public TokenUsage extractTokenUsageFromSseChunks(List<String> chunks) {
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;

        for (int i = chunks.size() - 1; i >= 0; i--) {
            String chunk = chunks.get(i);
            String data = extractSseData(chunk);
            if (data == null || "[DONE]".equals(data)) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(data);
                JsonNode usage = root.get("usage");
                if (usage != null && !usage.isNull()) {
                    promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                    completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                    totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : promptTokens + completionTokens;
                    return new TokenUsage(promptTokens, completionTokens, totalTokens);
                }
            } catch (JsonProcessingException e) {
                continue;
            }
        }

        return new TokenUsage(promptTokens, completionTokens, totalTokens);
    }

    private String extractSseData(String chunk) {
        for (String line : chunk.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring(5).trim();
                if (!data.isEmpty()) {
                    return data;
                }
            }
        }
        return null;
    }
}
