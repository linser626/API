package com.airelay.billing.service;

import com.airelay.billing.dto.BalanceOverview;
import com.airelay.billing.dto.RechargeRequest;
import com.airelay.billing.dto.TransactionQuery;
import com.airelay.billing.entity.BalanceTransaction;
import com.airelay.billing.mapper.BalanceTransactionMapper;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.payment.entity.Order;
import com.airelay.payment.service.PaymentService;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class BillingService {

    private final BalanceTransactionMapper balanceTransactionMapper;
    private final UserMapper userMapper;
    private final PaymentService paymentService;

    public BillingService(BalanceTransactionMapper balanceTransactionMapper,
                          UserMapper userMapper,
                          @Lazy PaymentService paymentService) {
        this.balanceTransactionMapper = balanceTransactionMapper;
        this.userMapper = userMapper;
        this.paymentService = paymentService;
    }

    public BalanceOverview getBalanceOverview(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal totalRecharge = BigDecimal.ZERO;
        BigDecimal totalConsume = BigDecimal.ZERO;
        BigDecimal totalGift = BigDecimal.ZERO;

        for (BalanceTransaction t : balanceTransactionMapper.selectList(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getUserId, userId)
                        .eq(BalanceTransaction::getType, "recharge")
                        .select(BalanceTransaction::getAmount))) {
            totalRecharge = totalRecharge.add(t.getAmount());
        }

        for (BalanceTransaction t : balanceTransactionMapper.selectList(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getUserId, userId)
                        .eq(BalanceTransaction::getType, "consume")
                        .select(BalanceTransaction::getAmount))) {
            totalConsume = totalConsume.add(t.getAmount().abs());
        }

        for (BalanceTransaction t : balanceTransactionMapper.selectList(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getUserId, userId)
                        .eq(BalanceTransaction::getType, "gift")
                        .select(BalanceTransaction::getAmount))) {
            totalGift = totalGift.add(t.getAmount());
        }

        return BalanceOverview.builder()
                .balance(user.getBalance())
                .totalRecharge(totalRecharge)
                .totalConsume(totalConsume)
                .totalGift(totalGift)
                .build();
    }

    public IPage<BalanceTransaction> getTransactionList(Long userId, TransactionQuery query) {
        Page<BalanceTransaction> pageParam = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<BalanceTransaction> wrapper = new LambdaQueryWrapper<BalanceTransaction>()
                .eq(BalanceTransaction::getUserId, userId);

        if (query.getType() != null && !query.getType().trim().isEmpty()) {
            wrapper.eq(BalanceTransaction::getType, query.getType());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(BalanceTransaction::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(BalanceTransaction::getCreatedAt, query.getEndTime());
        }
        wrapper.orderByDesc(BalanceTransaction::getCreatedAt);

        return balanceTransactionMapper.selectPage(pageParam, wrapper);
    }

    @Transactional
    public Order recharge(Long userId, RechargeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        return paymentService.createOrder(
                userId,
                "recharge",
                request.getAmount(),
                request.getPaymentMethod(),
                request.getCouponCode(),
                null,
                "余额充值"
        );
    }

    @Transactional
    public BalanceTransaction consume(Long userId, BigDecimal amount, String description, Long orderId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消费金额必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        return deductBalance(userId, amount, description, orderId);
    }

    @Transactional
    public BalanceTransaction rechargeBalance(Long userId, BigDecimal amount, String description, Long orderId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "充值金额必须大于0");
        }

        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        user.setBalance(balanceAfter);
        userMapper.updateById(user);

        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(userId);
        transaction.setType("recharge");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setOrderId(orderId);
        transaction.setCreatedAt(LocalDateTime.now());
        balanceTransactionMapper.insert(transaction);

        log.info("充值成功: userId={}, amount={}, balanceBefore={}, balanceAfter={}", userId, amount, balanceBefore, balanceAfter);
        return transaction;
    }

    @Transactional
    public BalanceTransaction refund(Long userId, BigDecimal amount, String description, Long orderId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额必须大于0");
        }

        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        user.setBalance(balanceAfter);
        userMapper.updateById(user);

        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(userId);
        transaction.setType("refund");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setOrderId(orderId);
        transaction.setCreatedAt(LocalDateTime.now());
        balanceTransactionMapper.insert(transaction);

        log.info("退款成功: userId={}, amount={}, balanceBefore={}, balanceAfter={}", userId, amount, balanceBefore, balanceAfter);
        return transaction;
    }

    @Transactional
    public BalanceTransaction giftBalance(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "赠送金额必须大于0");
        }

        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        user.setBalance(balanceAfter);
        userMapper.updateById(user);

        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(userId);
        transaction.setType("gift");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        balanceTransactionMapper.insert(transaction);

        log.info("赠送余额: userId={}, amount={}, balanceBefore={}, balanceAfter={}", userId, amount, balanceBefore, balanceAfter);
        return transaction;
    }

    @Transactional
    public BalanceTransaction deductBalance(Long userId, BigDecimal amount, String description, Long orderId) {
        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        user.setBalance(balanceAfter);
        userMapper.updateById(user);

        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(userId);
        transaction.setType("consume");
        transaction.setAmount(amount.negate());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setOrderId(orderId);
        transaction.setCreatedAt(LocalDateTime.now());
        balanceTransactionMapper.insert(transaction);

        return transaction;
    }
}
