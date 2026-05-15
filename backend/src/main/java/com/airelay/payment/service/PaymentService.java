package com.airelay.payment.service;

import com.airelay.billing.service.BillingService;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.payment.dto.OrderVO;
import com.airelay.payment.entity.Coupon;
import com.airelay.payment.entity.Order;
import com.airelay.payment.entity.UserCoupon;
import com.airelay.payment.mapper.CouponMapper;
import com.airelay.payment.mapper.OrderMapper;
import com.airelay.payment.mapper.UserCouponMapper;
import com.airelay.subscription.service.SubscriptionService;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.UserMapper;
import com.airelay.user.service.ReferralService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderMapper orderMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;
    private final BillingService billingService;
    private final SubscriptionService subscriptionService;
    private final ReferralService referralService;

    @Transactional
    public Order createOrder(Long userId, String type, BigDecimal amount, String paymentMethod,
                             String couponCode, Long planId, String description) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal originalAmount = amount;
        BigDecimal discountAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setType(type);
        order.setOriginalAmount(originalAmount);
        order.setDiscountAmount(discountAmount);
        order.setAmount(originalAmount.subtract(discountAmount));
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("pending");
        order.setPlanId(planId);
        order.setDescription(description);
        orderMapper.insert(order);

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            discountAmount = applyCoupon(order.getId(), couponCode);
            order.setDiscountAmount(discountAmount);
            order.setAmount(originalAmount.subtract(discountAmount));
            if (order.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                order.setAmount(BigDecimal.ZERO);
            }
            orderMapper.updateById(order);
        }

        log.info("创建订单: orderNo={}, userId={}, type={}, amount={}", order.getOrderNo(), userId, type, order.getAmount());
        return order;
    }

    public Order getOrderByNo(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    public IPage<OrderVO> getUserOrders(Long userId, int page, int size) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        IPage<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        return orderPage.convert(this::convertToOrderVO);
    }

    @Transactional
    public void handlePaymentSuccess(String orderNo, String paymentNo, String paymentMethod) {
        Order order = getOrderByNo(orderNo);

        if (!"pending".equals(order.getPaymentStatus())) {
            log.warn("订单状态非待支付，忽略支付成功通知: orderNo={}, status={}", orderNo, order.getPaymentStatus());
            return;
        }

        order.setPaymentStatus("paid");
        order.setPaymentNo(paymentNo);
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        if ("recharge".equals(order.getType())) {
            billingService.rechargeBalance(order.getUserId(), order.getAmount(),
                    "余额充值 - 订单号: " + orderNo, order.getId());
        } else if ("subscription".equals(order.getType())) {
            if (order.getPlanId() != null) {
                subscriptionService.activateSubscription(order.getUserId(), order.getPlanId(), order.getId());
            } else {
                log.error("订阅订单缺少套餐ID: orderNo={}", orderNo);
            }
        }

        try {
            referralService.processCommission(order.getId(), order.getUserId(), order.getAmount());
        } catch (Exception e) {
            log.warn("处理推荐佣金失败: orderNo={}, error={}", orderNo, e.getMessage());
        }

        log.info("支付成功处理完成: orderNo={}, type={}", orderNo, order.getType());
    }

    @Transactional
    public void handlePaymentFailure(String orderNo) {
        Order order = getOrderByNo(orderNo);

        if (!"pending".equals(order.getPaymentStatus())) {
            return;
        }

        order.setPaymentStatus("failed");
        orderMapper.updateById(order);

        log.info("订单支付失败: orderNo={}", orderNo);
    }

    @Transactional
    public void refundOrder(String orderNo) {
        Order order = getOrderByNo(orderNo);

        if (!"paid".equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有已支付的订单才能退款");
        }

        order.setPaymentStatus("refunded");
        orderMapper.updateById(order);

        if ("recharge".equals(order.getType())) {
            billingService.refund(order.getUserId(), order.getAmount(),
                    "充值退款 - 订单号: " + orderNo, order.getId());
        }

        log.info("订单退款成功: orderNo={}", orderNo);
    }

    @Transactional
    public BigDecimal applyCoupon(Long orderId, String couponCode) {
        Coupon coupon = couponMapper.selectOne(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getCode, couponCode)
                        .eq(Coupon::getStatus, 1)
        );

        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "优惠券不存在或已失效");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "优惠券不在有效期内");
        }

        if (coupon.getTotalCount() != -1 && coupon.getUsedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "优惠券已被领完");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }

        if (order.getOriginalAmount().compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "订单金额不满足优惠券最低使用条件");
        }

        Long userUsageCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, order.getUserId())
                        .eq(UserCoupon::getCouponId, coupon.getId())
        );
        if (userUsageCount >= coupon.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "已达到优惠券使用上限");
        }

        BigDecimal discountAmount;
        if ("fixed".equals(coupon.getType())) {
            discountAmount = coupon.getValue();
        } else if ("percent".equals(coupon.getType())) {
            discountAmount = order.getOriginalAmount().multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                discountAmount = coupon.getMaxDiscount();
            }
        } else {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "优惠券类型无效");
        }

        if (discountAmount.compareTo(order.getOriginalAmount()) > 0) {
            discountAmount = order.getOriginalAmount();
        }

        if (coupon.getTotalCount() != -1) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponMapper.updateById(coupon);
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(order.getUserId());
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setOrderId(orderId);
        userCoupon.setStatus("used");
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);

        order.setCouponId(coupon.getId());
        orderMapper.updateById(order);

        log.info("优惠券使用成功: orderId={}, couponCode={}, discountAmount={}", orderId, couponCode, discountAmount);
        return discountAmount;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        return "ORD" + timestamp + random;
    }

    private OrderVO convertToOrderVO(Order order) {
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .type(order.getType())
                .amount(order.getAmount())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .description(order.getDescription())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}
