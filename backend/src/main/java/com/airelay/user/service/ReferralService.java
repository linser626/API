package com.airelay.user.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.user.dto.ReferralInfoVO;
import com.airelay.user.dto.ReferralRecordVO;
import com.airelay.user.entity.Referral;
import com.airelay.user.entity.ReferralRecord;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.ReferralMapper;
import com.airelay.user.mapper.ReferralRecordMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralMapper referralMapper;
    private final ReferralRecordMapper referralRecordMapper;
    private final UserMapper userMapper;

    @Value("${app.referral.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${app.referral.default-commission-rate:10.00}")
    private BigDecimal defaultCommissionRate;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    public ReferralInfoVO getReferralInfo(Long userId) {
        Referral referral = getOrCreateReferral(userId);

        List<ReferralRecord> recentRecords = referralRecordMapper.selectList(
                new LambdaQueryWrapper<ReferralRecord>()
                        .eq(ReferralRecord::getReferrerId, userId)
                        .orderByDesc(ReferralRecord::getCreatedAt)
                        .last("LIMIT 10")
        );

        List<ReferralRecordVO> recordVOs = new ArrayList<>();
        for (ReferralRecord record : recentRecords) {
            User referredUser = userMapper.selectById(record.getReferredId());
            String username = referredUser != null ? maskUsername(referredUser.getUsername()) : "未知用户";
            recordVOs.add(ReferralRecordVO.builder()
                    .referredUsername(username)
                    .orderAmount(record.getOrderAmount())
                    .commission(record.getCommission())
                    .status(record.getStatus())
                    .createdAt(record.getCreatedAt())
                    .build());
        }

        return ReferralInfoVO.builder()
                .referralCode(referral.getReferralCode())
                .referralLink(baseUrl + "/register?ref=" + referral.getReferralCode())
                .totalReferrals(referral.getTotalReferrals())
                .totalEarned(referral.getTotalEarned())
                .commissionRate(referral.getCommissionRate())
                .recentRecords(recordVOs)
                .build();
    }

    public String generateReferralCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            Long count = referralMapper.selectCount(
                    new LambdaQueryWrapper<Referral>().eq(Referral::getReferralCode, code)
            );
            if (count == 0) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成推荐码失败，请重试");
    }

    @Transactional
    public Referral getOrCreateReferral(Long userId) {
        Referral referral = referralMapper.selectOne(
                new LambdaQueryWrapper<Referral>().eq(Referral::getUserId, userId)
        );
        if (referral == null) {
            referral = new Referral();
            referral.setUserId(userId);
            referral.setReferralCode(generateReferralCode());
            referral.setTotalReferrals(0);
            referral.setTotalEarned(BigDecimal.ZERO);
            referral.setCommissionRate(defaultCommissionRate);
            referralMapper.insert(referral);

            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setReferralCode(referral.getReferralCode());
                userMapper.updateById(user);
            }
        }
        return referral;
    }

    @Transactional
    public void processReferral(Long userId, String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        Referral referrerReferral = referralMapper.selectOne(
                new LambdaQueryWrapper<Referral>().eq(Referral::getReferralCode, referralCode)
        );
        if (referrerReferral == null) {
            log.warn("推荐码不存在: {}", referralCode);
            return;
        }

        if (referrerReferral.getUserId().equals(userId)) {
            log.warn("用户不能使用自己的推荐码: userId={}", userId);
            return;
        }

        Referral userReferral = referralMapper.selectOne(
                new LambdaQueryWrapper<Referral>().eq(Referral::getUserId, userId)
        );
        if (userReferral != null && userReferral.getReferredBy() != null) {
            log.warn("用户已有推荐人: userId={}", userId);
            return;
        }

        if (userReferral == null) {
            userReferral = getOrCreateReferral(userId);
        }

        userReferral.setReferredBy(referrerReferral.getUserId());
        referralMapper.updateById(userReferral);

        referrerReferral.setTotalReferrals(referrerReferral.getTotalReferrals() + 1);
        referralMapper.updateById(referrerReferral);

        log.info("推荐关系建立: referrerId={}, referredId={}, code={}", referrerReferral.getUserId(), userId, referralCode);
    }

    @Transactional
    public void processCommission(Long orderId, Long userId, BigDecimal orderAmount) {
        Referral userReferral = referralMapper.selectOne(
                new LambdaQueryWrapper<Referral>().eq(Referral::getUserId, userId)
        );
        if (userReferral == null || userReferral.getReferredBy() == null) {
            return;
        }

        Long referrerId = userReferral.getReferredBy();

        Referral referrerReferral = referralMapper.selectOne(
                new LambdaQueryWrapper<Referral>().eq(Referral::getUserId, referrerId)
        );
        if (referrerReferral == null) {
            return;
        }

        BigDecimal commission = orderAmount.multiply(referrerReferral.getCommissionRate())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        if (commission.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        ReferralRecord record = new ReferralRecord();
        record.setReferrerId(referrerId);
        record.setReferredId(userId);
        record.setOrderId(orderId);
        record.setOrderAmount(orderAmount);
        record.setCommission(commission);
        record.setStatus("pending");
        referralRecordMapper.insert(record);

        referrerReferral.setTotalEarned(referrerReferral.getTotalEarned().add(commission));
        referralMapper.updateById(referrerReferral);

        User referrer = userMapper.selectById(referrerId);
        if (referrer != null) {
            referrer.setBalance(referrer.getBalance().add(commission));
            userMapper.updateById(referrer);
        }

        record.setStatus("paid");
        referralRecordMapper.updateById(record);

        log.info("推荐佣金处理完成: referrerId={}, orderId={}, orderAmount={}, commission={}",
                referrerId, orderId, orderAmount, commission);
    }

    public IPage<ReferralRecordVO> getReferralRecords(Long userId, int page, int size) {
        Page<ReferralRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ReferralRecord> wrapper = new LambdaQueryWrapper<ReferralRecord>()
                .eq(ReferralRecord::getReferrerId, userId)
                .orderByDesc(ReferralRecord::getCreatedAt);

        IPage<ReferralRecord> recordPage = referralRecordMapper.selectPage(pageParam, wrapper);

        return recordPage.convert(record -> {
            User referredUser = userMapper.selectById(record.getReferredId());
            String username = referredUser != null ? maskUsername(referredUser.getUsername()) : "未知用户";
            return ReferralRecordVO.builder()
                    .referredUsername(username)
                    .orderAmount(record.getOrderAmount())
                    .commission(record.getCommission())
                    .status(record.getStatus())
                    .createdAt(record.getCreatedAt())
                    .build();
        });
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}
