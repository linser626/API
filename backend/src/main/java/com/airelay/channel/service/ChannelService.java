package com.airelay.channel.service;

import com.airelay.channel.dto.ChannelCreateRequest;
import com.airelay.channel.dto.ChannelUpdateRequest;
import com.airelay.channel.entity.Channel;
import com.airelay.channel.mapper.ChannelMapper;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelMapper channelMapper;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public Channel createChannel(ChannelCreateRequest request) {
        validateModelsJson(request.getModels());

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setBaseUrl(request.getBaseUrl());
        channel.setApiKey(request.getApiKey());
        channel.setModels(request.getModels());
        channel.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        channel.setWeight(request.getWeight() != null ? request.getWeight() : 1);
        channel.setStatus(1);
        channel.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        channel.setTimeoutMs(request.getTimeoutMs() != null ? request.getTimeoutMs() : 30000);
        channel.setResponseTimeMs(0);
        channel.setSuccessRate(BigDecimal.valueOf(100.00));
        channel.setTotalRequests(0L);
        channel.setFailedRequests(0L);

        channelMapper.insert(channel);
        return channel;
    }

    public Channel updateChannel(Long id, ChannelUpdateRequest request) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通道不存在");
        }

        if (request.getName() != null) {
            channel.setName(request.getName());
        }
        if (request.getBaseUrl() != null) {
            channel.setBaseUrl(request.getBaseUrl());
        }
        if (request.getApiKey() != null) {
            channel.setApiKey(request.getApiKey());
        }
        if (request.getModels() != null) {
            validateModelsJson(request.getModels());
            channel.setModels(request.getModels());
        }
        if (request.getPriority() != null) {
            channel.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            channel.setWeight(request.getWeight());
        }
        if (request.getStatus() != null) {
            channel.setStatus(request.getStatus());
        }
        if (request.getMaxRetries() != null) {
            channel.setMaxRetries(request.getMaxRetries());
        }
        if (request.getTimeoutMs() != null) {
            channel.setTimeoutMs(request.getTimeoutMs());
        }

        channelMapper.updateById(channel);
        return channel;
    }

    public void deleteChannel(Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通道不存在");
        }
        channelMapper.deleteById(id);
    }

    public Channel getChannelById(Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通道不存在");
        }
        return channel;
    }

    public IPage<Channel> listChannels(int page, int size, String type, Integer status) {
        Page<Channel> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Channel::getType, type);
        }
        if (status != null) {
            wrapper.eq(Channel::getStatus, status);
        }
        wrapper.orderByDesc(Channel::getPriority).orderByAsc(Channel::getId);
        return channelMapper.selectPage(pageParam, wrapper);
    }

    public List<Channel> getAvailableChannels(String model) {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Channel::getStatus, 1);
        wrapper.like(Channel::getModels, "\"" + model + "\"");
        wrapper.orderByDesc(Channel::getPriority);
        List<Channel> channels = channelMapper.selectList(wrapper);

        channels = channels.stream()
                .filter(ch -> isModelSupported(ch, model))
                .toList();

        return channels;
    }

    public void updateChannelStats(Long channelId, boolean success, long latencyMs) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            return;
        }

        channel.setTotalRequests(channel.getTotalRequests() + 1);
        if (!success) {
            channel.setFailedRequests(channel.getFailedRequests() + 1);
        }

        if (success && latencyMs > 0) {
            int currentResponseTime = channel.getResponseTimeMs() != null ? channel.getResponseTimeMs() : 0;
            int newResponseTime = (int) ((currentResponseTime * (channel.getTotalRequests() - 1) + latencyMs) / channel.getTotalRequests());
            channel.setResponseTimeMs(newResponseTime);
        }

        if (channel.getTotalRequests() > 0) {
            BigDecimal successRate = BigDecimal.valueOf(channel.getTotalRequests() - channel.getFailedRequests())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(channel.getTotalRequests()), 2, RoundingMode.HALF_UP);
            channel.setSuccessRate(successRate);
        }

        if (!success && channel.getSuccessRate().compareTo(BigDecimal.valueOf(50)) < 0) {
            channel.setStatus(2);
        }

        channelMapper.updateById(channel);
    }

    public boolean testChannel(Long id) {
        Channel channel = getChannelById(id);
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(channel.getBaseUrl())
                    .build();

            Mono<String> response = webClient.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(channel.getTimeoutMs()));

            response.block();
            return true;
        } catch (Exception e) {
            log.warn("通道测试失败, channelId={}, error={}", id, e.getMessage());
            return false;
        }
    }

    private boolean isModelSupported(Channel channel, String model) {
        try {
            List<String> modelList = objectMapper.readValue(channel.getModels(), new TypeReference<>() {});
            return modelList.contains(model);
        } catch (JsonProcessingException e) {
            return channel.getModels().contains(model);
        }
    }

    private void validateModelsJson(String models) {
        try {
            objectMapper.readValue(models, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型列表必须是有效的JSON数组格式");
        }
    }
}
