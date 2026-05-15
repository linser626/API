package com.airelay.apikey.service;

import com.airelay.apikey.entity.ApiKey;
import com.airelay.apikey.mapper.ApiKeyMapper;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.user.entity.Plan;
import com.airelay.user.entity.Subscription;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.PlanMapper;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public ApiKey createApiKey(Long userId, String name) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        int maxApiKeys = getMaxApiKeys(userId);
        long currentKeyCount = apiKeyMapper.selectCount(
                new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getUserId, userId)
        );
        if (currentKeyCount >= maxApiKeys) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "API密钥数量已达上限，最多允许" + maxApiKeys + "个");
        }

        String keyValue = generateApiKey();

        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setKeyValue(keyValue);
        apiKey.setName(name);
        apiKey.setStatus(1);
        apiKey.setRateLimitRpm(60);
        apiKey.setRateLimitTpm(100000);
        apiKey.setTotalQuota(-1L);
        apiKey.setUsedQuota(0L);

        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    public List<ApiKey> listApiKeys(Long userId) {
        return apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getUserId, userId)
                        .orderByDesc(ApiKey::getCreatedAt)
        );
    }

    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getKeyValue, keyValue)
        );
    }

    public void revokeApiKey(Long userId, Long keyId) {
        ApiKey apiKey = apiKeyMapper.selectById(keyId);
        if (apiKey == null || !apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在");
        }
        apiKeyMapper.deleteById(keyId);
    }

    public ApiKey updateApiKey(Long userId, Long keyId, String name, Integer rateLimitRpm, Integer rateLimitTpm) {
        ApiKey apiKey = apiKeyMapper.selectById(keyId);
        if (apiKey == null || !apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在");
        }

        if (name != null) {
            apiKey.setName(name);
        }
        if (rateLimitRpm != null) {
            apiKey.setRateLimitRpm(rateLimitRpm);
        }
        if (rateLimitTpm != null) {
            apiKey.setRateLimitTpm(rateLimitTpm);
        }

        apiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    public ApiKey validateApiKey(String keyValue) {
        ApiKey apiKey = getApiKeyByValue(keyValue);
        if (apiKey == null) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API密钥不存在");
        }
        if (apiKey.getStatus() == 0) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API密钥已被禁用");
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API密钥已过期");
        }

        User user = userMapper.selectById(apiKey.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API密钥所属用户不可用");
        }

        return apiKey;
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[36];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return "sk-" + randomPart.substring(0, 48);
    }

    private int getMaxApiKeys(Long userId) {
        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getEndTime)
                        .last("LIMIT 1")
        );
        if (subscription == null) {
            return 1;
        }
        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            return 1;
        }
        return plan.getMaxApiKeys();
    }
}
