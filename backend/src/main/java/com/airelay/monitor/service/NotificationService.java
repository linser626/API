package com.airelay.monitor.service;

import com.airelay.user.entity.Subscription;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_KEY_PREFIX = "notification:user:";
    private static final double BALANCE_WARNING_THRESHOLD = 1.0;
    private static final int SUBSCRIPTION_WARNING_DAYS = 3;

    public enum NotificationType {
        BALANCE_LOW,
        SUBSCRIPTION_EXPIRING,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        COUPON_RECEIVED
    }

    public void sendNotification(Long userId, NotificationType type, String title, String content) {
        try {
            String id = UUID.randomUUID().toString();
            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("id", id);
            notification.put("userId", userId);
            notification.put("type", type.name());
            notification.put("title", title);
            notification.put("content", content);
            notification.put("read", false);
            notification.put("createdAt", LocalDateTime.now().toString());

            String json = objectMapper.writeValueAsString(notification);
            String key = NOTIFICATION_KEY_PREFIX + userId;
            stringRedisTemplate.opsForList().leftPush(key, json);

            log.debug("发送通知: userId={}, type={}, title={}", userId, type, title);
        } catch (JsonProcessingException e) {
            log.error("发送通知失败: userId={}, type={}, error={}", userId, type, e.getMessage());
        }
    }

    public List<Map<String, Object>> getUserNotifications(Long userId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return Collections.emptyList();
        }

        List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, size - 1);
        if (jsonList == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> notifications = new ArrayList<>();
        for (String json : jsonList) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> notification = objectMapper.readValue(json, Map.class);
                notifications.add(notification);
            } catch (JsonProcessingException e) {
                log.warn("解析通知失败: {}", e.getMessage());
            }
        }

        return notifications;
    }

    public void markAsRead(Long userId, String notificationId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return;
        }

        List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, size - 1);
        if (jsonList == null) {
            return;
        }

        for (int i = 0; i < jsonList.size(); i++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> notification = objectMapper.readValue(jsonList.get(i), Map.class);
                if (notificationId.equals(notification.get("id"))) {
                    notification.put("read", true);
                    String updatedJson = objectMapper.writeValueAsString(notification);
                    stringRedisTemplate.opsForList().set(key, i, updatedJson);
                    break;
                }
            } catch (JsonProcessingException e) {
                log.warn("更新通知状态失败: {}", e.getMessage());
            }
        }
    }

    public void markAllAsRead(Long userId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return;
        }

        List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, size - 1);
        if (jsonList == null) {
            return;
        }

        for (int i = 0; i < jsonList.size(); i++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> notification = objectMapper.readValue(jsonList.get(i), Map.class);
                if (!Boolean.TRUE.equals(notification.get("read"))) {
                    notification.put("read", true);
                    String updatedJson = objectMapper.writeValueAsString(notification);
                    stringRedisTemplate.opsForList().set(key, i, updatedJson);
                }
            } catch (JsonProcessingException e) {
                log.warn("更新通知状态失败: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void checkAndSendWarnings() {
        try {
            checkBalanceWarnings();
            checkSubscriptionWarnings();
        } catch (Exception e) {
            log.error("通知检查任务异常: {}", e.getMessage());
        }
    }

    private void checkBalanceWarnings() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(User::getBalance, BALANCE_WARNING_THRESHOLD);
        wrapper.gt(User::getBalance, 0);
        List<User> lowBalanceUsers = userMapper.selectList(wrapper);

        for (User user : lowBalanceUsers) {
            sendNotification(user.getId(), NotificationType.BALANCE_LOW,
                    "余额不足提醒",
                    "您的账户余额为 " + user.getBalance() + " 元，低于预警阈值，请及时充值。");
        }
    }

    private void checkSubscriptionWarnings() {
        LocalDateTime warningThreshold = LocalDateTime.now().plus(SUBSCRIPTION_WARNING_DAYS, ChronoUnit.DAYS);

        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getStatus, "active");
        wrapper.between(Subscription::getEndTime, LocalDateTime.now(), warningThreshold);
        List<Subscription> expiringSubscriptions = subscriptionMapper.selectList(wrapper);

        for (Subscription sub : expiringSubscriptions) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndTime());
            sendNotification(sub.getUserId(), NotificationType.SUBSCRIPTION_EXPIRING,
                    "订阅即将到期",
                    "您的订阅将在 " + daysLeft + " 天后到期，请及时续费。");
        }
    }
}
