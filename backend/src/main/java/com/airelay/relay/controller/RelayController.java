package com.airelay.relay.controller;

import com.airelay.channel.entity.Channel;
import com.airelay.channel.service.ChannelService;
import com.airelay.common.Constants;
import com.airelay.relay.service.RelayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Tag(name = "AI模型中转")
@RestController
@RequiredArgsConstructor
public class RelayController {

    private final RelayService relayService;
    private final ChannelService channelService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Chat Completion - OpenAI兼容")
    @PostMapping(value = "/v1/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> relayChatCompletion(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
            HttpServletRequest request) {

        String apiKey = extractApiKey(authorization, xApiKey);
        String model = extractModelFromBody(requestBody);
        String ipAddress = getClientIpAddress(request);

        boolean isStreaming = isStreamingRequest(requestBody);
        if (isStreaming) {
            Flux<String> stream = relayService.relayStreamingChatCompletion(requestBody, apiKey, model, ipAddress);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("X-Accel-Buffering", "no")
                    .body(stream);
        }

        return relayService.relayChatCompletion(requestBody, apiKey, model, ipAddress);
    }

    @Operation(summary = "Text Completion - OpenAI兼容")
    @PostMapping(value = "/v1/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> relayCompletion(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
            HttpServletRequest request) {

        String apiKey = extractApiKey(authorization, xApiKey);
        String model = extractModelFromBody(requestBody);
        String ipAddress = getClientIpAddress(request);

        boolean isStreaming = isStreamingRequest(requestBody);
        if (isStreaming) {
            Flux<String> stream = relayService.relayStreamingChatCompletion(requestBody, apiKey, model, ipAddress);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("X-Accel-Buffering", "no")
                    .body(stream);
        }

        return relayService.relayCompletion(requestBody, apiKey, model, ipAddress);
    }

    @Operation(summary = "Embeddings - OpenAI兼容")
    @PostMapping("/v1/embeddings")
    public ResponseEntity<byte[]> relayEmbedding(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
            HttpServletRequest request) {

        String apiKey = extractApiKey(authorization, xApiKey);
        String model = extractModelFromBody(requestBody);
        String ipAddress = getClientIpAddress(request);

        return relayService.relayEmbedding(requestBody, apiKey, model, ipAddress);
    }

    @Operation(summary = "Image Generation - OpenAI兼容")
    @PostMapping("/v1/images/generations")
    public ResponseEntity<byte[]> relayImage(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
            HttpServletRequest request) {

        String apiKey = extractApiKey(authorization, xApiKey);
        String model = extractModelFromBody(requestBody);
        String ipAddress = getClientIpAddress(request);

        return relayService.relayImage(requestBody, apiKey, model, ipAddress);
    }

    @Operation(summary = "列出可用模型 - OpenAI兼容")
    @GetMapping("/v1/models")
    public ResponseEntity<byte[]> listModels(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey) {

        if (authorization != null || xApiKey != null) {
            try {
                String apiKey = extractApiKey(authorization, xApiKey);
                if (apiKey != null && !apiKey.isEmpty()) {
                    Set<String> modelSet = new LinkedHashSet<>();

                    List<Channel> allActive = channelService.listChannels(1, 1000, null, 1).getRecords();
                    for (Channel channel : allActive) {
                        try {
                            List<String> models = objectMapper.readValue(channel.getModels(), new TypeReference<>() {});
                            modelSet.addAll(models);
                        } catch (JsonProcessingException e) {
                            log.warn("解析通道模型列表失败, channelId={}", channel.getId());
                        }
                    }

                    List<Map<String, Object>> modelList = new ArrayList<>();
                    for (String modelId : modelSet) {
                        Map<String, Object> modelObj = new LinkedHashMap<>();
                        modelObj.put("id", modelId);
                        modelObj.put("object", "model");
                        modelObj.put("created", System.currentTimeMillis() / 1000);
                        modelObj.put("owned_by", "airelay");
                        modelList.add(modelObj);
                    }

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("object", "list");
                    response.put("data", modelList);

                    byte[] bytes = objectMapper.writeValueAsBytes(response);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(bytes);
                }
            } catch (Exception e) {
                log.warn("列出模型失败: {}", e.getMessage());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("object", "list");
        response.put("data", Collections.emptyList());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bytes);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().body(new byte[0]);
        }
    }

    private String extractApiKey(String authorization, String xApiKey) {
        if (xApiKey != null && !xApiKey.isEmpty()) {
            return xApiKey;
        }
        if (authorization != null && authorization.startsWith(Constants.API_KEY_PREFIX)) {
            return authorization;
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (token.startsWith(Constants.API_KEY_PREFIX)) {
                return token;
            }
        }
        return authorization != null ? authorization : "";
    }

    @SuppressWarnings("unchecked")
    private String extractModelFromBody(String requestBody) {
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
            Object model = bodyMap.get("model");
            return model != null ? model.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isStreamingRequest(String requestBody) {
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
            Object stream = bodyMap.get("stream");
            return Boolean.TRUE.equals(stream);
        } catch (Exception e) {
            return false;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
