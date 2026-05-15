package com.airelay.channel.service;

import com.airelay.channel.entity.Channel;
import com.airelay.channel.mapper.ChannelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelHealthService {

    private final ChannelMapper channelMapper;
    private final WebClient webClient;

    @Scheduled(fixedRate = 300000)
    public void checkChannelHealth() {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Channel::getStatus, 1);
        List<Channel> activeChannels = channelMapper.selectList(wrapper);

        if (activeChannels.isEmpty()) {
            log.debug("没有活跃通道需要健康检查");
            return;
        }

        int healthyCount = 0;
        int unhealthyCount = 0;

        for (Channel channel : activeChannels) {
            try {
                boolean healthy = testChannel(channel);
                if (healthy) {
                    healthyCount++;
                } else {
                    unhealthyCount++;
                    channel.setStatus(2);
                    channelMapper.updateById(channel);
                    log.warn("通道健康检查失败, channelId={}, channelName={}, 已标记为异常", channel.getId(), channel.getName());
                }
            } catch (Exception e) {
                unhealthyCount++;
                log.error("通道健康检查异常, channelId={}, error={}", channel.getId(), e.getMessage());
            }
        }

        log.info("通道健康检查完成: 总计={}, 健康={}, 异常={}", activeChannels.size(), healthyCount, unhealthyCount);
    }

    public boolean testChannel(Channel channel) {
        try {
            String testUrl = buildTestUrl(channel);
            long startTime = System.currentTimeMillis();

            String response = webClient.get()
                    .uri(testUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(channel.getTimeoutMs() != null ? channel.getTimeoutMs() : 10000))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;

            if (response != null) {
                channel.setResponseTimeMs((int) responseTime);
                channelMapper.updateById(channel);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.debug("通道测试失败, channelId={}, error={}", channel.getId(), e.getMessage());
            return false;
        }
    }

    private String buildTestUrl(Channel channel) {
        String baseUrl = channel.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String type = channel.getType();
        if ("claude".equalsIgnoreCase(type)) {
            return baseUrl + "/v1/models";
        } else if ("gemini".equalsIgnoreCase(type)) {
            return baseUrl + "/v1/models?key=" + channel.getApiKey();
        } else {
            return baseUrl + "/v1/models";
        }
    }
}
