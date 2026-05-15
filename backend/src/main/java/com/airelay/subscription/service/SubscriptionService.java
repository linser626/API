package com.airelay.subscription.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.payment.entity.Order;
import com.airelay.payment.service.PaymentService;
import com.airelay.subscription.dto.PlanVO;
import com.airelay.subscription.dto.SubscribeRequest;
import com.airelay.subscription.dto.SubscriptionVO;
import com.airelay.subscription.dto.UserQuotaDTO;
import com.airelay.user.entity.Plan;
import com.airelay.user.entity.Subscription;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.PlanMapper;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SubscriptionService {

    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public SubscriptionService(PlanMapper planMapper,
                               SubscriptionMapper subscriptionMapper,
                               UserMapper userMapper,
                               @Lazy PaymentService paymentService,
                               ObjectMapper objectMapper) {
        this.planMapper = planMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public List<PlanVO> listPlans() {
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<Plan>()
                .eq(Plan::getStatus, 1)
                .orderByAsc(Plan::getSortOrder);
        List<Plan> plans = planMapper.selectList(wrapper);

        List<PlanVO> result = new ArrayList<>();
        for (Plan plan : plans) {
            result.add(convertToPlanVO(plan));
        }
        return result;
    }

    public SubscriptionVO getCurrentSubscription(Long userId) {
        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (subscription == null) {
            return null;
        }

        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            return null;
        }

        User user = userMapper.selectById(userId);

        return SubscriptionVO.builder()
                .id(subscription.getId())
                .planId(plan.getId())
                .planName(plan.getName())
                .status(subscription.getStatus())
                .startTime(subscription.getStartTime())
                .endTime(subscription.getEndTime())
                .autoRenew(subscription.getAutoRenew())
                .tokenQuota(plan.getTokenQuota())
                .usedQuota(user != null ? user.getUsedQuota() : 0L)
                .rateLimitRpm(plan.getRateLimitRpm())
                .rateLimitTpm(plan.getRateLimitTpm())
                .maxApiKeys(plan.getMaxApiKeys())
                .build();
    }

    @Transactional
    public Order subscribe(Long userId, SubscribeRequest request) {
        Plan plan = planMapper.selectById(request.getPlanId());
        if (plan == null || plan.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在或已下架");
        }

        return paymentService.createOrder(
                userId,
                "subscription",
                plan.getPrice(),
                request.getPaymentMethod(),
                request.getCouponCode(),
                plan.getId(),
                "订阅套餐: " + plan.getName()
        );
    }

    @Transactional
    public void activateSubscription(Long userId, Long planId, Long orderId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }

        List<Subscription> activeSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
        );

        for (Subscription existing : activeSubscriptions) {
            existing.setStatus("cancelled");
            subscriptionMapper.updateById(existing);
        }

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlanId(planId);
        subscription.setStatus("active");
        subscription.setStartTime(LocalDateTime.now());
        subscription.setEndTime(LocalDateTime.now().plusDays(plan.getDurationDays()));
        subscription.setAutoRenew(0);
        subscriptionMapper.insert(subscription);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setTotalQuota(plan.getTokenQuota());
            user.setUsedQuota(0L);
            userMapper.updateById(user);
        }

        log.info("订阅激活成功: userId={}, planId={}, subscriptionId={}", userId, planId, subscription.getId());
    }

    @Transactional
    public void cancelSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionMapper.selectById(subscriptionId);
        if (subscription == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在");
        }
        if (!subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订阅");
        }
        if (!"active".equals(subscription.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订阅已非活跃状态");
        }

        subscription.setAutoRenew(0);
        subscriptionMapper.updateById(subscription);

        log.info("取消自动续费: userId={}, subscriptionId={}", userId, subscriptionId);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void checkAndExpireSubscriptions() {
        List<Subscription> expiredSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, "active")
                        .lt(Subscription::getEndTime, LocalDateTime.now())
        );

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus("expired");
            subscriptionMapper.updateById(subscription);
            log.info("订阅已过期: userId={}, subscriptionId={}", subscription.getUserId(), subscription.getId());
        }
    }

    public UserQuotaDTO getUserQuotaAndLimits(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (subscription == null) {
            return UserQuotaDTO.builder()
                    .tokenQuota(0L)
                    .usedQuota(user.getUsedQuota())
                    .rateLimitRpm(0)
                    .rateLimitTpm(0)
                    .maxApiKeys(0)
                    .subscriptionStatus("none")
                    .build();
        }

        Plan plan = planMapper.selectById(subscription.getPlanId());

        return UserQuotaDTO.builder()
                .tokenQuota(plan != null ? plan.getTokenQuota() : 0L)
                .usedQuota(user.getUsedQuota())
                .rateLimitRpm(plan != null ? plan.getRateLimitRpm() : 0)
                .rateLimitTpm(plan != null ? plan.getRateLimitTpm() : 0)
                .maxApiKeys(plan != null ? plan.getMaxApiKeys() : 0)
                .subscriptionStatus(subscription.getStatus())
                .build();
    }

    private PlanVO convertToPlanVO(Plan plan) {
        List<String> features = new ArrayList<>();
        if (plan.getFeatures() != null && !plan.getFeatures().trim().isEmpty()) {
            try {
                features = objectMapper.readValue(plan.getFeatures(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.warn("解析套餐特性失败: planId={}, features={}", plan.getId(), plan.getFeatures());
            }
        }

        return PlanVO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .tokenQuota(plan.getTokenQuota())
                .rateLimitRpm(plan.getRateLimitRpm())
                .rateLimitTpm(plan.getRateLimitTpm())
                .maxApiKeys(plan.getMaxApiKeys())
                .features(features)
                .isDefault(plan.getIsDefault())
                .sortOrder(plan.getSortOrder())
                .status(plan.getStatus())
                .build();
    }
}
