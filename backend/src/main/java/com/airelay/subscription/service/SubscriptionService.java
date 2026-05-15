package com.airelay.subscription.service;

import com.airelay.billing.service.BillingService;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.monitor.service.NotificationService;
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

import java.math.BigDecimal;
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
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public SubscriptionService(PlanMapper planMapper,
                               SubscriptionMapper subscriptionMapper,
                               UserMapper userMapper,
                               @Lazy PaymentService paymentService,
                               @Lazy BillingService billingService,
                               NotificationService notificationService,
                               ObjectMapper objectMapper) {
        this.planMapper = planMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
        this.paymentService = paymentService;
        this.billingService = billingService;
        this.notificationService = notificationService;
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
                .autoRenewPaymentMethod(subscription.getAutoRenewPaymentMethod())
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
        subscription.setAutoRenewPaymentMethod(null);
        subscriptionMapper.updateById(subscription);

        log.info("取消自动续费: userId={}, subscriptionId={}", userId, subscriptionId);
    }

    @Transactional
    public void enableAutoRenew(Long userId, String paymentMethod) {
        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getEndTime)
                        .last("LIMIT 1")
        );

        if (subscription == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "没有活跃的订阅");
        }

        if (paymentMethod == null || (!paymentMethod.equals("alipay") && !paymentMethod.equals("wechat") && !paymentMethod.equals("balance"))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式无效，支持: alipay/wechat/balance");
        }

        subscription.setAutoRenew(1);
        subscription.setAutoRenewPaymentMethod(paymentMethod);
        subscriptionMapper.updateById(subscription);

        log.info("启用自动续费: userId={}, subscriptionId={}, paymentMethod={}", userId, subscription.getId(), paymentMethod);
    }

    @Transactional
    public void disableAutoRenew(Long userId) {
        Subscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getEndTime)
                        .last("LIMIT 1")
        );

        if (subscription == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "没有活跃的订阅");
        }

        subscription.setAutoRenew(0);
        subscription.setAutoRenewPaymentMethod(null);
        subscriptionMapper.updateById(subscription);

        log.info("禁用自动续费: userId={}, subscriptionId={}", userId, subscription.getId());
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void checkAndExpireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime renewalThreshold = now.plusHours(24);

        List<Subscription> renewingSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, "active")
                        .eq(Subscription::getAutoRenew, 1)
                        .between(Subscription::getEndTime, now, renewalThreshold)
        );

        for (Subscription subscription : renewingSubscriptions) {
            try {
                attemptAutoRenewal(subscription);
            } catch (Exception e) {
                log.error("自动续费失败: userId={}, subscriptionId={}, error={}",
                        subscription.getUserId(), subscription.getId(), e.getMessage());
                subscription.setAutoRenew(0);
                subscription.setAutoRenewPaymentMethod(null);
                subscriptionMapper.updateById(subscription);

                notificationService.sendNotification(
                        subscription.getUserId(),
                        NotificationService.NotificationType.PAYMENT_FAILED,
                        "自动续费失败",
                        "您的订阅自动续费失败，请检查余额或手动续费。"
                );
            }
        }

        List<Subscription> expiredSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, "active")
                        .lt(Subscription::getEndTime, now)
        );

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus("expired");
            subscriptionMapper.updateById(subscription);
            log.info("订阅已过期: userId={}, subscriptionId={}", subscription.getUserId(), subscription.getId());

            notificationService.sendNotification(
                    subscription.getUserId(),
                    NotificationService.NotificationType.SUBSCRIPTION_EXPIRING,
                    "订阅已过期",
                    "您的订阅已过期，请及时续费以继续使用服务。"
            );
        }
    }

    @Transactional
    public void attemptAutoRenewal(Subscription subscription) {
        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在，无法续费");
        }

        User user = userMapper.selectById(subscription.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal renewalPrice = plan.getPrice();

        if (user.getBalance().compareTo(renewalPrice) >= 0) {
            billingService.deductBalance(
                    user.getId(),
                    renewalPrice,
                    "自动续费 - 套餐: " + plan.getName(),
                    null
            );

            subscription.setEndTime(subscription.getEndTime().plusDays(plan.getDurationDays()));
            subscriptionMapper.updateById(subscription);

            log.info("自动续费成功: userId={}, subscriptionId={}, amount={}", user.getId(), subscription.getId(), renewalPrice);

            notificationService.sendNotification(
                    user.getId(),
                    NotificationService.NotificationType.PAYMENT_SUCCESS,
                    "自动续费成功",
                    "您的订阅已自动续费成功，新到期时间: " + subscription.getEndTime().toString()
            );
        } else {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "余额不足，无法自动续费");
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
