package com.airelay.relay.service;

import com.airelay.channel.entity.Channel;
import com.airelay.relay.strategy.LoadBalanceStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadBalanceService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROUND_ROBIN_KEY_PREFIX = "relay:round_robin:";

    public Channel selectChannel(List<Channel> channels, LoadBalanceStrategy strategy) {
        if (channels == null || channels.isEmpty()) {
            return null;
        }
        if (channels.size() == 1) {
            return channels.get(0);
        }

        return switch (strategy) {
            case PRIORITY -> selectByPriority(channels);
            case WEIGHTED_RANDOM -> selectByWeightedRandom(channels);
            case ROUND_ROBIN -> selectByRoundRobin(channels);
            case LEAST_LATENCY -> selectByLeastLatency(channels);
        };
    }

    private Channel selectByPriority(List<Channel> channels) {
        return channels.get(0);
    }

    private Channel selectByWeightedRandom(List<Channel> channels) {
        int totalWeight = channels.stream()
                .mapToInt(ch -> ch.getWeight() != null ? ch.getWeight() : 1)
                .sum();

        if (totalWeight <= 0) {
            return channels.get(0);
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int accumulated = 0;

        for (Channel channel : channels) {
            int weight = channel.getWeight() != null ? channel.getWeight() : 1;
            accumulated += weight;
            if (random < accumulated) {
                return channel;
            }
        }

        return channels.get(channels.size() - 1);
    }

    private Channel selectByRoundRobin(List<Channel> channels) {
        String key = ROUND_ROBIN_KEY_PREFIX + channels.get(0).getType();
        Long counter = stringRedisTemplate.opsForValue().increment(key);
        if (counter != null && counter == 1L) {
            stringRedisTemplate.expire(key, java.time.Duration.ofMinutes(10));
        }
        int index = (int) ((counter != null ? counter : 0) % channels.size());
        return channels.get(index);
    }

    private Channel selectByLeastLatency(List<Channel> channels) {
        return channels.stream()
                .min((c1, c2) -> {
                    int t1 = c1.getResponseTimeMs() != null ? c1.getResponseTimeMs() : Integer.MAX_VALUE;
                    int t2 = c2.getResponseTimeMs() != null ? c2.getResponseTimeMs() : Integer.MAX_VALUE;
                    return Integer.compare(t1, t2);
                })
                .orElse(channels.get(0));
    }
}
