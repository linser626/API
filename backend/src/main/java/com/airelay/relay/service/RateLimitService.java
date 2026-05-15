package com.airelay.relay.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RPM_KEY_PREFIX = "rate_limit:rpm:";
    private static final String TPM_KEY_PREFIX = "rate_limit:tpm:";

    public boolean checkRateLimit(Long apiKeyId, Integer rpm, Integer tpm) {
        if (rpm != null && rpm > 0) {
            if (!checkRpmLimit(apiKeyId, rpm)) {
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "请求频率超限，每分钟最多" + rpm + "次请求");
            }
        }

        if (tpm != null && tpm > 0) {
            if (!checkTpmLimit(apiKeyId, tpm)) {
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "Token频率超限，每分钟最多" + tpm + "个Token");
            }
        }

        return true;
    }

    private boolean checkRpmLimit(Long apiKeyId, int rpm) {
        String key = RPM_KEY_PREFIX + apiKeyId;
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return current == null || current <= rpm;
    }

    private boolean checkTpmLimit(Long apiKeyId, int tpm) {
        String key = TPM_KEY_PREFIX + apiKeyId;
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        int current = currentValue != null ? Integer.parseInt(currentValue) : 0;
        return current <= tpm;
    }

    public void recordTokenUsage(Long apiKeyId, int tokens) {
        String key = TPM_KEY_PREFIX + apiKeyId;
        stringRedisTemplate.opsForValue().increment(key, tokens);
        Long ttl = stringRedisTemplate.getExpire(key);
        if (ttl == null || ttl < 0) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
    }
}
