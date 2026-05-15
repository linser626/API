package com.airelay.coupon.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.coupon.dto.CouponCreateRequest;
import com.airelay.coupon.dto.CouponVO;
import com.airelay.coupon.dto.UserCouponVO;
import com.airelay.payment.entity.Coupon;
import com.airelay.payment.entity.UserCoupon;
import com.airelay.payment.mapper.CouponMapper;
import com.airelay.payment.mapper.UserCouponMapper;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Transactional
    public Coupon createCoupon(CouponCreateRequest request) {
        Long codeCount = couponMapper.selectCount(
                new LambdaQueryWrapper<Coupon>().eq(Coupon::getCode, request.getCode())
        );
        if (codeCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券代码已存在");
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }

        if ("percent".equals(request.getType())) {
            if (request.getValue().compareTo(BigDecimal.ZERO) <= 0 || request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "百分比折扣值必须在0-100之间");
            }
        } else if ("fixed".equals(request.getType())) {
            if (request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "固定金额必须大于0");
            }
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券类型无效，仅支持fixed或percent");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(request.getCode());
        coupon.setName(request.getName());
        coupon.setType(request.getType());
        coupon.setValue(request.getValue());
        coupon.setMinAmount(request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setTotalCount(request.getTotalCount() != null ? request.getTotalCount() : -1);
        coupon.setUsedCount(0);
        coupon.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        coupon.setStartTime(request.getStartTime());
        coupon.setEndTime(request.getEndTime());
        coupon.setStatus(1);

        couponMapper.insert(coupon);
        log.info("创建优惠券: code={}, name={}, type={}", coupon.getCode(), coupon.getName(), coupon.getType());
        return coupon;
    }

    @Transactional
    public Coupon updateCoupon(Long id, CouponCreateRequest request) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }

        if (request.getCode() != null) {
            Long codeCount = couponMapper.selectCount(
                    new LambdaQueryWrapper<Coupon>()
                            .eq(Coupon::getCode, request.getCode())
                            .ne(Coupon::getId, id)
            );
            if (codeCount > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券代码已存在");
            }
            coupon.setCode(request.getCode());
        }
        if (request.getName() != null) {
            coupon.setName(request.getName());
        }
        if (request.getType() != null) {
            coupon.setType(request.getType());
        }
        if (request.getValue() != null) {
            coupon.setValue(request.getValue());
        }
        if (request.getMinAmount() != null) {
            coupon.setMinAmount(request.getMinAmount());
        }
        if (request.getMaxDiscount() != null) {
            coupon.setMaxDiscount(request.getMaxDiscount());
        }
        if (request.getTotalCount() != null) {
            coupon.setTotalCount(request.getTotalCount());
        }
        if (request.getPerUserLimit() != null) {
            coupon.setPerUserLimit(request.getPerUserLimit());
        }
        if (request.getStartTime() != null) {
            coupon.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            coupon.setEndTime(request.getEndTime());
        }

        couponMapper.updateById(coupon);
        log.info("更新优惠券: id={}, code={}", id, coupon.getCode());
        return coupon;
    }

    public IPage<CouponVO> listCoupons(int page, int size, Integer status) {
        Page<Coupon> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreatedAt);

        IPage<Coupon> couponPage = couponMapper.selectPage(pageParam, wrapper);
        return couponPage.convert(this::convertToCouponVO);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        coupon.setStatus(0);
        couponMapper.updateById(coupon);
        log.info("删除优惠券: id={}, code={}", id, coupon.getCode());
    }

    @Transactional
    public UserCoupon redeemCoupon(Long userId, String couponCode) {
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

        Long userUsageCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, coupon.getId())
        );
        if (userUsageCount >= coupon.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "已达到优惠券领取上限");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setStatus("unused");
        userCouponMapper.insert(userCoupon);

        if (coupon.getTotalCount() != -1) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponMapper.updateById(coupon);
        }

        log.info("用户领取优惠券: userId={}, couponCode={}", userId, couponCode);
        return userCoupon;
    }

    public List<UserCouponVO> getUserCoupons(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreatedAt);

        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon != null) {
                result.add(convertToUserCouponVO(uc, coupon));
            }
        }
        return result;
    }

    public Coupon validateCoupon(Long userId, String couponCode, BigDecimal orderAmount) {
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

        if (orderAmount != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "订单金额不满足优惠券最低使用条件");
        }

        Long userUsageCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, coupon.getId())
        );
        if (userUsageCount >= coupon.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "已达到优惠券使用上限");
        }

        return coupon;
    }

    @Transactional
    public void useCoupon(Long userId, Long couponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
                        .eq(UserCoupon::getStatus, "unused")
                        .last("LIMIT 1")
        );
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.COUPON_INVALID, "未找到可使用的优惠券");
        }

        userCoupon.setStatus("used");
        userCoupon.setOrderId(orderId);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);

        log.info("使用优惠券: userId={}, couponId={}, orderId={}", userId, couponId, orderId);
    }

    private CouponVO convertToCouponVO(Coupon coupon) {
        return CouponVO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minAmount(coupon.getMinAmount())
                .maxDiscount(coupon.getMaxDiscount())
                .totalCount(coupon.getTotalCount())
                .usedCount(coupon.getUsedCount())
                .perUserLimit(coupon.getPerUserLimit())
                .startTime(coupon.getStartTime())
                .endTime(coupon.getEndTime())
                .status(coupon.getStatus())
                .build();
    }

    private UserCouponVO convertToUserCouponVO(UserCoupon uc, Coupon coupon) {
        return UserCouponVO.builder()
                .id(uc.getId())
                .couponId(uc.getCouponId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minAmount(coupon.getMinAmount())
                .maxDiscount(coupon.getMaxDiscount())
                .status(uc.getStatus())
                .usedAt(uc.getUsedAt())
                .build();
    }
}
