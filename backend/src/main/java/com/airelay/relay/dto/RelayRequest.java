package com.airelay.relay.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RelayRequest {

    private String model;
    private List<Map<String, Object>> messages;
    private String prompt;
    private Boolean stream;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private List<String> stop;
    private Integer n;
    private String input;
    private String prompt2;
    private String size;
    private String quality;
    private String style;
    private String responseFormat;

    private final Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (model != null) map.put("model", model);
        if (messages != null) map.put("messages", messages);
        if (prompt != null) map.put("prompt", prompt);
        if (stream != null) map.put("stream", stream);
        if (temperature != null) map.put("temperature", temperature);
        if (maxTokens != null) map.put("max_tokens", maxTokens);
        if (topP != null) map.put("top_p", topP);
        if (frequencyPenalty != null) map.put("frequency_penalty", frequencyPenalty);
        if (presencePenalty != null) map.put("presence_penalty", presencePenalty);
        if (stop != null) map.put("stop", stop);
        if (n != null) map.put("n", n);
        if (input != null) map.put("input", input);
        if (size != null) map.put("size", size);
        if (quality != null) map.put("quality", quality);
        if (style != null) map.put("style", style);
        if (responseFormat != null) map.put("response_format", responseFormat);
        map.putAll(additionalProperties);
        return map;
    }
}
