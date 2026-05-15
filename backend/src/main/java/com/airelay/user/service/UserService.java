package com.airelay.user.service;

import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.security.JwtTokenProvider;
import com.airelay.user.dto.*;
import com.airelay.user.entity.Plan;
import com.airelay.user.entity.Subscription;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.PlanMapper;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ReferralService referralService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public User register(RegisterRequest request) {
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalQuota(0L);
        user.setUsedQuota(0L);
        user.setAvatar("");
        userMapper.insert(user);

        Plan defaultPlan = planMapper.selectOne(
                new LambdaQueryWrapper<Plan>()
                        .eq(Plan::getIsDefault, 1)
                        .eq(Plan::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (defaultPlan != null) {
            Subscription subscription = new Subscription();
            subscription.setUserId(user.getId());
            subscription.setPlanId(defaultPlan.getId());
            subscription.setStatus("active");
            subscription.setStartTime(LocalDateTime.now());
            subscription.setEndTime(LocalDateTime.now().plusDays(defaultPlan.getDurationDays()));
            subscription.setAutoRenew(0);
            subscriptionMapper.insert(subscription);

            user.setTotalQuota(defaultPlan.getTokenQuota());
            userMapper.updateById(user);
        }

        referralService.getOrCreateReferral(user.getId());

        if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
            referralService.processReferral(user.getId(), request.getReferralCode().trim());
        }

        return user;
    }

    public TokenResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String accessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), Collections.singletonList(user.getRole())
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), Collections.singletonList(user.getRole())
        );

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, request.getEmail())
                            .ne(User::getId, id)
            );
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userMapper.updateById(user);
        return user;
    }

    public void changePassword(Long id, PasswordChangeRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public IPage<User> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword)
            );
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(pageParam, wrapper);
    }

    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
