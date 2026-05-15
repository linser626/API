package com.airelay.relay.service;

import com.airelay.apikey.entity.ApiKey;
import com.airelay.apikey.mapper.ApiKeyMapper;
import com.airelay.apikey.service.ApiKeyService;
import com.airelay.channel.entity.Channel;
import com.airelay.channel.service.ChannelService;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.relay.dto.ModerationResult;
import com.airelay.relay.dto.TokenUsage;
import com.airelay.relay.entity.ModelPrice;
import com.airelay.relay.entity.RequestLog;
import com.airelay.relay.handler.SseHandler;
import com.airelay.relay.mapper.ModelPriceMapper;
import com.airelay.relay.mapper.RequestLogMapper;
import com.airelay.relay.strategy.LoadBalanceStrategy;
import com.airelay.team.entity.Team;
import com.airelay.team.entity.TeamApiKey;
import com.airelay.team.mapper.TeamMapper;
import com.airelay.team.mapper.TeamApiKeyMapper;
import com.airelay.team.service.TeamService;
import com.airelay.user.entity.Subscription;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelayService {

    private final ApiKeyService apiKeyService;
    private final ApiKeyMapper apiKeyMapper;
    private final ChannelService channelService;
    private final LoadBalanceService loadBalanceService;
    private final TokenCountService tokenCountService;
    private final RateLimitService rateLimitService;
    private final SseHandler sseHandler;
    private final ContentModerationService contentModerationService;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ModelPriceMapper modelPriceMapper;
    private final RequestLogMapper requestLogMapper;
    private final TeamService teamService;
    private final TeamMapper teamMapper;
    private final TeamApiKeyMapper teamApiKeyMapper;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    private static final LoadBalanceStrategy DEFAULT_STRATEGY = LoadBalanceStrategy.PRIORITY;

    public ResponseEntity<byte[]> relayChatCompletion(String requestBody, String apiKey, String model, String ipAddress) {
        return relayRequest(requestBody, apiKey, model, ipAddress, "chat", "/v1/chat/completions");
    }

    public ResponseEntity<byte[]> relayCompletion(String requestBody, String apiKey, String model, String ipAddress) {
        return relayRequest(requestBody, apiKey, model, ipAddress, "completion", "/v1/completions");
    }

    public ResponseEntity<byte[]> relayEmbedding(String requestBody, String apiKey, String model, String ipAddress) {
        return relayRequest(requestBody, apiKey, model, ipAddress, "embedding", "/v1/embeddings");
    }

    public ResponseEntity<byte[]> relayImage(String requestBody, String apiKey, String model, String ipAddress) {
        return relayRequest(requestBody, apiKey, model, ipAddress, "image", "/v1/images/generations");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<byte[]> relayRequest(String requestBody, String apiKey, String model, String ipAddress, String requestType, String upstreamPath) {
        boolean isTeamKey = apiKey != null && apiKey.startsWith("sk-team-");

        if (isTeamKey) {
            return relayTeamRequest(requestBody, apiKey, model, ipAddress, requestType, upstreamPath);
        }

        ApiKey validatedKey = apiKeyService.validateApiKey(apiKey);
        User user = userMapper.selectById(validatedKey.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户不可用");
        }

        checkSubscriptionAndQuota(user);

        rateLimitService.checkRateLimit(validatedKey.getId(), validatedKey.getRateLimitRpm(), validatedKey.getRateLimitTpm());

        if (contentModerationService.isEnabled()) {
            ModerationResult moderationResult = contentModerationService.checkContent(requestBody);
            if (!moderationResult.isPassed()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, moderationResult.getReason());
            }
        }

        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
                actualModel = (String) bodyMap.getOrDefault("model", "gpt-3.5-turbo");
            } catch (Exception e) {
                actualModel = "gpt-3.5-turbo";
            }
        }

        List<Channel> channels = channelService.getAvailableChannels(actualModel);
        if (channels.isEmpty()) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道支持模型: " + actualModel);
        }

        boolean isStreaming = false;
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
            isStreaming = Boolean.TRUE.equals(bodyMap.get("stream"));
        } catch (Exception ignored) {
        }

        if (isStreaming) {
            return relayStreamingRequest(requestBody, validatedKey, user, actualModel, ipAddress, requestType, upstreamPath, channels);
        }

        return relayNonStreamingRequest(requestBody, validatedKey, user, actualModel, ipAddress, requestType, upstreamPath, channels);
    }

    private ResponseEntity<byte[]> relayNonStreamingRequest(String requestBody, ApiKey apiKey, User user, String model, String ipAddress, String requestType, String upstreamPath, List<Channel> channels) {
        Exception lastException = null;
        List<Channel> availableChannels = new ArrayList<>(channels);

        for (int attempt = 0; attempt < availableChannels.size(); attempt++) {
            Channel channel = loadBalanceService.selectChannel(availableChannels, DEFAULT_STRATEGY);
            if (channel == null) {
                break;
            }
            availableChannels.remove(channel);

            long startTime = System.currentTimeMillis();
            try {
                String url = buildUpstreamUrl(channel, upstreamPath);

                WebClient webClient = buildWebClientForChannel(channel, url);

                String modifiedBody = replaceModelInBody(requestBody, model);

                byte[] responseBytes = webClient.post()
                        .bodyValue(modifiedBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response ->
                                response.bodyToMono(String.class)
                                        .flatMap(errorBody -> Mono.error(new RuntimeException("上游返回错误: " + response.statusCode() + " - " + errorBody)))
                        )
                        .bodyToMono(byte[].class)
                        .timeout(Duration.ofMillis(channel.getTimeoutMs()))
                        .block();

                long latencyMs = System.currentTimeMillis() - startTime;

                String responseBody = new String(responseBytes != null ? responseBytes : new byte[0]);
                TokenUsage tokenUsage = tokenCountService.extractTokenUsage(responseBody);

                if (tokenUsage.getTotalTokens() == 0) {
                    int estimated = tokenCountService.countTokens(model, requestBody);
                    tokenUsage = new TokenUsage(estimated, 0, estimated);
                }

                BigDecimal cost = calculateCost(model, tokenUsage);
                deductBalance(user, cost);
                updateQuotaUsage(user, tokenUsage.getTotalTokens());

                rateLimitService.recordTokenUsage(apiKey.getId(), tokenUsage.getTotalTokens());

                channelService.updateChannelStats(channel.getId(), true, latencyMs);

                logRequest(user.getId(), apiKey.getId(), channel.getId(), model, requestType, tokenUsage, cost, latencyMs, "success", null, ipAddress);

                updateApiKeyLastUsed(apiKey.getId());

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBytes);

            } catch (Exception e) {
                lastException = e;
                long latencyMs = System.currentTimeMillis() - startTime;
                channelService.updateChannelStats(channel.getId(), false, latencyMs);
                log.warn("通道请求失败, channelId={}, model={}, attempt={}/{}, error={}",
                        channel.getId(), model, attempt + 1, channels.size(), e.getMessage());
                logRequest(user.getId(), apiKey.getId(), channel.getId(), model, requestType,
                        new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
            }
        }

        throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "所有通道请求失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private ResponseEntity<byte[]> relayStreamingRequest(String requestBody, ApiKey apiKey, User user, String model, String ipAddress, String requestType, String upstreamPath, List<Channel> channels) {
        Channel channel = loadBalanceService.selectChannel(channels, DEFAULT_STRATEGY);
        if (channel == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道");
        }

        long startTime = System.currentTimeMillis();

        try {
            String url = buildUpstreamUrl(channel, upstreamPath);

            WebClient webClient = buildWebClientForChannel(channel, url);

            String modifiedBody = replaceModelInBody(requestBody, model);

            Flux<String> stream = webClient.post()
                    .bodyValue(modifiedBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMillis(channel.getTimeoutMs()));

            List<String> chunks = new ArrayList<>();
            AtomicReference<TokenUsage> usageRef = new AtomicReference<>(new TokenUsage(0, 0, 0));

            byte[] resultBytes = stream
                    .doOnNext(chunk -> {
                        synchronized (chunks) {
                            chunks.add(chunk);
                        }
                    })
                    .doOnComplete(() -> {
                        long latencyMs = System.currentTimeMillis() - startTime;
                        TokenUsage usage = sseHandler.extractUsageFromChunks(chunks);
                        usageRef.set(usage);

                        if (usage.getTotalTokens() == 0) {
                            int estimated = tokenCountService.countTokens(model, requestBody);
                            usage = new TokenUsage(estimated, 0, estimated);
                            usageRef.set(usage);
                        }

                        BigDecimal cost = calculateCost(model, usage);
                        deductBalance(user, cost);
                        updateQuotaUsage(user, usage.getTotalTokens());
                        rateLimitService.recordTokenUsage(apiKey.getId(), usage.getTotalTokens());
                        channelService.updateChannelStats(channel.getId(), true, latencyMs);
                        logRequest(user.getId(), apiKey.getId(), channel.getId(), model, requestType, usage, cost, latencyMs, "success", null, ipAddress);
                        updateApiKeyLastUsed(apiKey.getId());
                    })
                    .doOnError(e -> {
                        long latencyMs = System.currentTimeMillis() - startTime;
                        channelService.updateChannelStats(channel.getId(), false, latencyMs);
                        logRequest(user.getId(), apiKey.getId(), channel.getId(), model, requestType,
                                new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
                    })
                    .collectList()
                    .map(chunkList -> String.join("", chunkList).getBytes())
                    .block();

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(resultBytes != null ? resultBytes : new byte[0]);

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            channelService.updateChannelStats(channel.getId(), false, latencyMs);
            logRequest(user.getId(), apiKey.getId(), channel.getId(), model, requestType,
                    new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "流式请求失败: " + e.getMessage());
        }
    }

    public Flux<String> relayStreamingChatCompletion(String requestBody, String apiKey, String model, String ipAddress) {
        boolean isTeamKey = apiKey != null && apiKey.startsWith("sk-team-");

        if (isTeamKey) {
            return relayTeamStreamingChatCompletion(requestBody, apiKey, model, ipAddress);
        }

        ApiKey validatedKey = apiKeyService.validateApiKey(apiKey);
        User user = userMapper.selectById(validatedKey.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户不可用");
        }

        checkSubscriptionAndQuota(user);
        rateLimitService.checkRateLimit(validatedKey.getId(), validatedKey.getRateLimitRpm(), validatedKey.getRateLimitTpm());

        if (contentModerationService.isEnabled()) {
            ModerationResult moderationResult = contentModerationService.checkContent(requestBody);
            if (!moderationResult.isPassed()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, moderationResult.getReason());
            }
        }

        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
                actualModel = (String) bodyMap.getOrDefault("model", "gpt-3.5-turbo");
            } catch (Exception e) {
                actualModel = "gpt-3.5-turbo";
            }
        }

        List<Channel> channels = channelService.getAvailableChannels(actualModel);
        if (channels.isEmpty()) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道支持模型: " + actualModel);
        }

        Channel channel = loadBalanceService.selectChannel(channels, DEFAULT_STRATEGY);
        if (channel == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道");
        }

        long startTime = System.currentTimeMillis();
        String url = buildUpstreamUrl(channel, "/v1/chat/completions");

        WebClient webClient = buildWebClientForChannel(channel, url);

        String modifiedBody = replaceModelInBody(requestBody, actualModel);

        List<String> chunks = new ArrayList<>();

        return webClient.post()
                .bodyValue(modifiedBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(channel.getTimeoutMs()))
                .doOnNext(chunk -> {
                    synchronized (chunks) {
                        chunks.add(chunk);
                    }
                })
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    TokenUsage usage = sseHandler.extractUsageFromChunks(chunks);
                    if (usage.getTotalTokens() == 0) {
                        int estimated = tokenCountService.countTokens(actualModel, requestBody);
                        usage = new TokenUsage(estimated, 0, estimated);
                    }
                    BigDecimal cost = calculateCost(actualModel, usage);
                    deductBalance(user, cost);
                    updateQuotaUsage(user, usage.getTotalTokens());
                    rateLimitService.recordTokenUsage(validatedKey.getId(), usage.getTotalTokens());
                    channelService.updateChannelStats(channel.getId(), true, latencyMs);
                    logRequest(user.getId(), validatedKey.getId(), channel.getId(), actualModel, "chat", usage, cost, latencyMs, "success", null, ipAddress);
                    updateApiKeyLastUsed(validatedKey.getId());
                })
                .doOnError(e -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    channelService.updateChannelStats(channel.getId(), false, latencyMs);
                    logRequest(user.getId(), validatedKey.getId(), channel.getId(), actualModel, "chat",
                            new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
                });
    }

    @SuppressWarnings("unchecked")
    private Flux<String> relayTeamStreamingChatCompletion(String requestBody, String apiKey, String model, String ipAddress) {
        TeamApiKey teamApiKey = teamService.validateTeamApiKey(apiKey);
        Team team = teamMapper.selectById(teamApiKey.getTeamId());
        if (team == null || team.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "团队不可用");
        }

        User owner = userMapper.selectById(team.getOwnerId());
        if (owner == null || owner.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "团队所有者不可用");
        }

        rateLimitService.checkRateLimit(teamApiKey.getId(), teamApiKey.getRateLimitRpm(), teamApiKey.getRateLimitTpm());

        if (contentModerationService.isEnabled()) {
            ModerationResult moderationResult = contentModerationService.checkContent(requestBody);
            if (!moderationResult.isPassed()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, moderationResult.getReason());
            }
        }

        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
                actualModel = (String) bodyMap.getOrDefault("model", "gpt-3.5-turbo");
            } catch (Exception e) {
                actualModel = "gpt-3.5-turbo";
            }
        }

        List<Channel> channels = channelService.getAvailableChannels(actualModel);
        if (channels.isEmpty()) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道支持模型: " + actualModel);
        }

        Channel channel = loadBalanceService.selectChannel(channels, DEFAULT_STRATEGY);
        if (channel == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道");
        }

        long startTime = System.currentTimeMillis();
        String url = buildUpstreamUrl(channel, "/v1/chat/completions");
        WebClient webClient = buildWebClientForChannel(channel, url);
        String modifiedBody = replaceModelInBody(requestBody, actualModel);

        List<String> chunks = new ArrayList<>();

        return webClient.post()
                .bodyValue(modifiedBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(channel.getTimeoutMs()))
                .doOnNext(chunk -> {
                    synchronized (chunks) {
                        chunks.add(chunk);
                    }
                })
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    TokenUsage usage = sseHandler.extractUsageFromChunks(chunks);
                    if (usage.getTotalTokens() == 0) {
                        int estimated = tokenCountService.countTokens(actualModel, requestBody);
                        usage = new TokenUsage(estimated, 0, estimated);
                    }
                    BigDecimal cost = calculateCost(actualModel, usage);
                    teamService.deductTeamBalance(team.getId(), cost);
                    teamService.updateTeamApiKeyUsage(teamApiKey.getId(), usage.getTotalTokens());
                    rateLimitService.recordTokenUsage(teamApiKey.getId(), usage.getTotalTokens());
                    channelService.updateChannelStats(channel.getId(), true, latencyMs);
                    logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), actualModel, "chat", usage, cost, latencyMs, "success", null, ipAddress);
                })
                .doOnError(e -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    channelService.updateChannelStats(channel.getId(), false, latencyMs);
                    logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), actualModel, "chat",
                            new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
                });
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<byte[]> relayTeamRequest(String requestBody, String apiKey, String model, String ipAddress, String requestType, String upstreamPath) {
        TeamApiKey teamApiKey = teamService.validateTeamApiKey(apiKey);
        Team team = teamMapper.selectById(teamApiKey.getTeamId());
        if (team == null || team.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "团队不可用");
        }

        User owner = userMapper.selectById(team.getOwnerId());
        if (owner == null || owner.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "团队所有者不可用");
        }

        rateLimitService.checkRateLimit(teamApiKey.getId(), teamApiKey.getRateLimitRpm(), teamApiKey.getRateLimitTpm());

        if (contentModerationService.isEnabled()) {
            ModerationResult moderationResult = contentModerationService.checkContent(requestBody);
            if (!moderationResult.isPassed()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, moderationResult.getReason());
            }
        }

        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
                actualModel = (String) bodyMap.getOrDefault("model", "gpt-3.5-turbo");
            } catch (Exception e) {
                actualModel = "gpt-3.5-turbo";
            }
        }

        List<Channel> channels = channelService.getAvailableChannels(actualModel);
        if (channels.isEmpty()) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道支持模型: " + actualModel);
        }

        boolean isStreaming = false;
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
            isStreaming = Boolean.TRUE.equals(bodyMap.get("stream"));
        } catch (Exception ignored) {
        }

        if (isStreaming) {
            return relayTeamStreamingRequest(requestBody, teamApiKey, team, owner, actualModel, ipAddress, requestType, upstreamPath, channels);
        }

        return relayTeamNonStreamingRequest(requestBody, teamApiKey, team, owner, actualModel, ipAddress, requestType, upstreamPath, channels);
    }

    private ResponseEntity<byte[]> relayTeamNonStreamingRequest(String requestBody, TeamApiKey teamApiKey, Team team, User owner, String model, String ipAddress, String requestType, String upstreamPath, List<Channel> channels) {
        Exception lastException = null;
        List<Channel> availableChannels = new ArrayList<>(channels);

        for (int attempt = 0; attempt < availableChannels.size(); attempt++) {
            Channel channel = loadBalanceService.selectChannel(availableChannels, DEFAULT_STRATEGY);
            if (channel == null) {
                break;
            }
            availableChannels.remove(channel);

            long startTime = System.currentTimeMillis();
            try {
                String url = buildUpstreamUrl(channel, upstreamPath);
                WebClient webClient = buildWebClientForChannel(channel, url);
                String modifiedBody = replaceModelInBody(requestBody, model);

                byte[] responseBytes = webClient.post()
                        .bodyValue(modifiedBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response ->
                                response.bodyToMono(String.class)
                                        .flatMap(errorBody -> Mono.error(new RuntimeException("上游返回错误: " + response.statusCode() + " - " + errorBody)))
                        )
                        .bodyToMono(byte[].class)
                        .timeout(Duration.ofMillis(channel.getTimeoutMs()))
                        .block();

                long latencyMs = System.currentTimeMillis() - startTime;
                String responseBody = new String(responseBytes != null ? responseBytes : new byte[0]);
                TokenUsage tokenUsage = tokenCountService.extractTokenUsage(responseBody);

                if (tokenUsage.getTotalTokens() == 0) {
                    int estimated = tokenCountService.countTokens(model, requestBody);
                    tokenUsage = new TokenUsage(estimated, 0, estimated);
                }

                BigDecimal cost = calculateCost(model, tokenUsage);
                teamService.deductTeamBalance(team.getId(), cost);
                teamService.updateTeamApiKeyUsage(teamApiKey.getId(), tokenUsage.getTotalTokens());
                rateLimitService.recordTokenUsage(teamApiKey.getId(), tokenUsage.getTotalTokens());
                channelService.updateChannelStats(channel.getId(), true, latencyMs);
                logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), model, requestType, tokenUsage, cost, latencyMs, "success", null, ipAddress);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBytes);

            } catch (Exception e) {
                lastException = e;
                long latencyMs = System.currentTimeMillis() - startTime;
                channelService.updateChannelStats(channel.getId(), false, latencyMs);
                log.warn("通道请求失败, channelId={}, model={}, attempt={}/{}, error={}",
                        channel.getId(), model, attempt + 1, channels.size(), e.getMessage());
                logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), model, requestType,
                        new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
            }
        }

        throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "所有通道请求失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private ResponseEntity<byte[]> relayTeamStreamingRequest(String requestBody, TeamApiKey teamApiKey, Team team, User owner, String model, String ipAddress, String requestType, String upstreamPath, List<Channel> channels) {
        Channel channel = loadBalanceService.selectChannel(channels, DEFAULT_STRATEGY);
        if (channel == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "没有可用的通道");
        }

        long startTime = System.currentTimeMillis();

        try {
            String url = buildUpstreamUrl(channel, upstreamPath);
            WebClient webClient = buildWebClientForChannel(channel, url);
            String modifiedBody = replaceModelInBody(requestBody, model);

            Flux<String> stream = webClient.post()
                    .bodyValue(modifiedBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMillis(channel.getTimeoutMs()));

            List<String> chunks = new ArrayList<>();
            AtomicReference<TokenUsage> usageRef = new AtomicReference<>(new TokenUsage(0, 0, 0));

            byte[] resultBytes = stream
                    .doOnNext(chunk -> {
                        synchronized (chunks) {
                            chunks.add(chunk);
                        }
                    })
                    .doOnComplete(() -> {
                        long latencyMs = System.currentTimeMillis() - startTime;
                        TokenUsage usage = sseHandler.extractUsageFromChunks(chunks);
                        usageRef.set(usage);

                        if (usage.getTotalTokens() == 0) {
                            int estimated = tokenCountService.countTokens(model, requestBody);
                            usage = new TokenUsage(estimated, 0, estimated);
                            usageRef.set(usage);
                        }

                        BigDecimal cost = calculateCost(model, usage);
                        teamService.deductTeamBalance(team.getId(), cost);
                        teamService.updateTeamApiKeyUsage(teamApiKey.getId(), usage.getTotalTokens());
                        rateLimitService.recordTokenUsage(teamApiKey.getId(), usage.getTotalTokens());
                        channelService.updateChannelStats(channel.getId(), true, latencyMs);
                        logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), model, requestType, usage, cost, latencyMs, "success", null, ipAddress);
                    })
                    .doOnError(e -> {
                        long latencyMs = System.currentTimeMillis() - startTime;
                        channelService.updateChannelStats(channel.getId(), false, latencyMs);
                        logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), model, requestType,
                                new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
                    })
                    .collectList()
                    .map(chunkList -> String.join("", chunkList).getBytes())
                    .block();

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(resultBytes != null ? resultBytes : new byte[0]);

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            channelService.updateChannelStats(channel.getId(), false, latencyMs);
            logRequest(owner.getId(), teamApiKey.getId(), channel.getId(), model, requestType,
                    new TokenUsage(0, 0, 0), BigDecimal.ZERO, latencyMs, "fail", e.getMessage(), ipAddress);
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE, "流式请求失败: " + e.getMessage());
        }
    }

    private void checkSubscriptionAndQuota(User user) {
        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, user.getId())
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getEndTime)
                        .last("LIMIT 1")
        );

        if (subscription != null && subscription.getEndTime().isBefore(LocalDateTime.now())) {
            subscription.setStatus("expired");
            subscriptionMapper.updateById(subscription);
        }

        if (user.getTotalQuota() != null && user.getTotalQuota() > 0) {
            if (user.getUsedQuota() != null && user.getUsedQuota() >= user.getTotalQuota()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "配额已用尽");
            }
        }
    }

    private BigDecimal calculateCost(String model, TokenUsage usage) {
        ModelPrice modelPrice = modelPriceMapper.selectOne(
                new LambdaQueryWrapper<ModelPrice>()
                        .eq(ModelPrice::getModelId, model)
                        .eq(ModelPrice::getStatus, 1)
                        .last("LIMIT 1")
        );

        if (modelPrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = modelPrice.getInputPrice()
                .multiply(BigDecimal.valueOf(usage.getPromptTokens()))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = modelPrice.getOutputPrice()
                .multiply(BigDecimal.valueOf(usage.getCompletionTokens()))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);

        BigDecimal totalCost = inputCost.add(outputCost);

        if (modelPrice.getPriceMultiplier() != null) {
            totalCost = totalCost.multiply(modelPrice.getPriceMultiplier());
        }

        return totalCost.setScale(6, RoundingMode.HALF_UP);
    }

    @Transactional
    public void deductBalance(User user, BigDecimal cost) {
        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        User freshUser = userMapper.selectById(user.getId());
        if (freshUser.getBalance().compareTo(cost) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "余额不足，当前余额: " + freshUser.getBalance());
        }

        freshUser.setBalance(freshUser.getBalance().subtract(cost));
        userMapper.updateById(freshUser);
    }

    private void updateQuotaUsage(User user, int tokens) {
        if (tokens <= 0) {
            return;
        }
        User freshUser = userMapper.selectById(user.getId());
        freshUser.setUsedQuota(freshUser.getUsedQuota() + tokens);
        userMapper.updateById(freshUser);
    }

    private void logRequest(Long userId, Long apiKeyId, Long channelId, String model, String requestType,
                            TokenUsage tokenUsage, BigDecimal cost, long latencyMs, String status,
                            String errorMessage, String ipAddress) {
        try {
            RequestLog requestLog = new RequestLog();
            requestLog.setUserId(userId);
            requestLog.setApiKeyId(apiKeyId);
            requestLog.setChannelId(channelId);
            requestLog.setModel(model);
            requestLog.setRequestType(requestType);
            requestLog.setPromptTokens(tokenUsage.getPromptTokens());
            requestLog.setCompletionTokens(tokenUsage.getCompletionTokens());
            requestLog.setTotalTokens(tokenUsage.getTotalTokens());
            requestLog.setCost(cost);
            requestLog.setLatencyMs((int) latencyMs);
            requestLog.setStatus(status);
            requestLog.setErrorMessage(errorMessage != null && errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage);
            requestLog.setIpAddress(ipAddress);
            requestLogMapper.insert(requestLog);
        } catch (Exception e) {
            log.error("记录请求日志失败: {}", e.getMessage());
        }
    }

    private void updateApiKeyLastUsed(Long apiKeyId) {
        try {
            ApiKey key = apiKeyMapper.selectById(apiKeyId);
            if (key != null) {
                key.setLastUsedAt(LocalDateTime.now());
                apiKeyMapper.updateById(key);
            }
        } catch (Exception e) {
            log.debug("更新API密钥最后使用时间失败: {}", e.getMessage());
        }
    }

    private String buildUpstreamUrl(Channel channel, String path) {
        String base = channel.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + path;

        if ("gemini".equalsIgnoreCase(channel.getType())) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "key=" + channel.getApiKey();
        }

        return url;
    }

    private WebClient buildWebClientForChannel(Channel channel, String url) {
        String type = channel.getType();
        WebClient.Builder builder = webClientBuilder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if ("claude".equalsIgnoreCase(type)) {
            builder.defaultHeader("x-api-key", channel.getApiKey());
            builder.defaultHeader("anthropic-version", "2023-06-01");
        } else if ("gemini".equalsIgnoreCase(type)) {
            // API key is in URL query param, no auth header needed
        } else {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + channel.getApiKey());
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private String replaceModelInBody(String requestBody, String model) {
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
            bodyMap.put("model", model);
            return objectMapper.writeValueAsString(bodyMap);
        } catch (Exception e) {
            return requestBody;
        }
    }
}
