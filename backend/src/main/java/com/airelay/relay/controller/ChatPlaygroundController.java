package com.airelay.relay.controller;

import com.airelay.apikey.entity.ApiKey;
import com.airelay.apikey.service.ApiKeyService;
import com.airelay.channel.entity.Channel;
import com.airelay.channel.service.ChannelService;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.common.Result;
import com.airelay.relay.service.RelayService;
import com.airelay.security.SecurityUtils;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Tag(name = "在线测试")
@RestController
@RequestMapping("/api/playground")
@RequiredArgsConstructor
public class ChatPlaygroundController {

    private final RelayService relayService;
    private final ApiKeyService apiKeyService;
    private final ChannelService channelService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "发送聊天消息")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object chat(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户不可用");
        }

        String apiKeyValue = getOrCreateApiKey(userId);

        String model = (String) request.getOrDefault("model", "gpt-3.5-turbo");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.getOrDefault("messages", new ArrayList<>());
        boolean stream = Boolean.TRUE.equals(request.getOrDefault("stream", true));
        Double temperature = request.get("temperature") != null ? ((Number) request.get("temperature")).doubleValue() : null;
        Integer maxTokens = request.get("max_tokens") != null ? ((Number) request.get("max_tokens")).intValue() : null;

        if (messages.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息列表不能为空");
        }

        List<Channel> channels = channelService.getAvailableChannels(model);
        if (channels.isEmpty()) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道支持模型: " + model);
        }

        Map<String, Object> relayBody = new LinkedHashMap<>();
        relayBody.put("model", model);
        relayBody.put("messages", messages);
        relayBody.put("stream", stream);
        if (temperature != null) {
            relayBody.put("temperature", temperature);
        }
        if (maxTokens != null) {
            relayBody.put("max_tokens", maxTokens);
        }

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(relayBody);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求体序列化失败");
        }

        if (stream) {
            Flux<String> streamResponse = relayService.relayStreamingChatCompletion(requestBody, apiKeyValue, model, "127.0.0.1");
            return streamResponse.map(data -> ServerSentEvent.<String>builder().data(data).build());
        }

        return relayService.relayChatCompletion(requestBody, apiKeyValue, model, "127.0.0.1");
    }

    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> getPlaygroundModels() {
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
            modelObj.put("name", modelId);
            modelList.add(modelObj);
        }

        return Result.ok(modelList);
    }

    private String getOrCreateApiKey(Long userId) {
        List<ApiKey> keys = apiKeyService.listApiKeys(userId);
        for (ApiKey key : keys) {
            if (key.getStatus() != null && key.getStatus() == 1) {
                return key.getKeyValue();
            }
        }

        ApiKey newKey = apiKeyService.createApiKey(userId, "playground-auto");
        return newKey.getKeyValue();
    }
}
