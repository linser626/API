package com.airelay.admin.service;

import com.airelay.admin.dto.ModelPriceCreateRequest;
import com.airelay.admin.dto.RevenueStatsDTO;
import com.airelay.admin.dto.UserStatsDTO;
import com.airelay.billing.entity.BalanceTransaction;
import com.airelay.billing.mapper.BalanceTransactionMapper;
import com.airelay.channel.entity.Channel;
import com.airelay.channel.mapper.ChannelMapper;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.monitor.dto.DailyUsageDTO;
import com.airelay.relay.entity.ModelPrice;
import com.airelay.relay.entity.RequestLog;
import com.airelay.relay.mapper.ModelPriceMapper;
import com.airelay.relay.mapper.RequestLogMapper;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final RequestLogMapper requestLogMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ModelPriceMapper modelPriceMapper;
    private final ChannelMapper channelMapper;
    private final PasswordEncoder passwordEncoder;

    public List<RevenueStatsDTO> getRevenueStats(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }

        LambdaQueryWrapper<BalanceTransaction> wrapper = new LambdaQueryWrapper<BalanceTransaction>()
                .ge(BalanceTransaction::getCreatedAt, startTime)
                .le(BalanceTransaction::getCreatedAt, endTime)
                .in(BalanceTransaction::getType, "recharge", "consume");
        List<BalanceTransaction> transactions = balanceTransactionMapper.selectList(wrapper);

        LocalDate start = startTime.toLocalDate();
        LocalDate end = endTime.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, List<BalanceTransaction>> dailyGroups = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dailyGroups.put(date.format(formatter), new ArrayList<>());
        }

        for (BalanceTransaction t : transactions) {
            String dateKey = t.getCreatedAt().toLocalDate().format(formatter);
            dailyGroups.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(t);
        }

        List<RevenueStatsDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<BalanceTransaction>> entry : dailyGroups.entrySet()) {
            List<BalanceTransaction> dayTx = entry.getValue();
            BigDecimal rechargeAmount = BigDecimal.ZERO;
            BigDecimal subscriptionAmount = BigDecimal.ZERO;
            long orderCount = 0;

            for (BalanceTransaction t : dayTx) {
                if ("recharge".equals(t.getType())) {
                    rechargeAmount = rechargeAmount.add(t.getAmount());
                    orderCount++;
                } else if ("consume".equals(t.getType())) {
                    subscriptionAmount = subscriptionAmount.add(t.getAmount().abs());
                    orderCount++;
                }
            }

            BigDecimal revenue = rechargeAmount.add(subscriptionAmount);

            result.add(RevenueStatsDTO.builder()
                    .date(entry.getKey())
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .rechargeAmount(rechargeAmount)
                    .subscriptionAmount(subscriptionAmount)
                    .build());
        }

        return result;
    }

    public List<UserStatsDTO> getUserStats(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }

        List<User> allUsers = userMapper.selectList(null);

        LocalDate start = startTime.toLocalDate();
        LocalDate end = endTime.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, List<User>> dailyNewUsers = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dailyNewUsers.put(date.format(formatter), new ArrayList<>());
        }

        for (User user : allUsers) {
            if (user.getCreatedAt() != null) {
                String dateKey = user.getCreatedAt().toLocalDate().format(formatter);
                if (dailyNewUsers.containsKey(dateKey)) {
                    dailyNewUsers.get(dateKey).add(user);
                }
            }
        }

        List<UserStatsDTO> result = new ArrayList<>();
        long cumulativeTotal = 0;

        for (Map.Entry<String, List<User>> entry : dailyNewUsers.entrySet()) {
            long newUsers = entry.getValue().size();
            cumulativeTotal += newUsers;

            LocalDate date = LocalDate.parse(entry.getKey(), formatter);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long activeUsers = requestLogMapper.selectCount(
                    new LambdaQueryWrapper<RequestLog>()
                            .ge(RequestLog::getCreatedAt, dayStart)
                            .le(RequestLog::getCreatedAt, dayEnd)
            );

            result.add(UserStatsDTO.builder()
                    .date(entry.getKey())
                    .newUsers(newUsers)
                    .activeUsers(activeUsers)
                    .totalUsers(cumulativeTotal)
                    .build());
        }

        return result;
    }

    public List<DailyUsageDTO> getRequestStats(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }

        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .ge(RequestLog::getCreatedAt, startTime)
                .le(RequestLog::getCreatedAt, endTime);
        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        LocalDate start = startTime.toLocalDate();
        LocalDate end = endTime.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, List<RequestLog>> dailyGroups = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dailyGroups.put(date.format(formatter), new ArrayList<>());
        }

        for (RequestLog l : logs) {
            String dateKey = l.getCreatedAt().toLocalDate().format(formatter);
            dailyGroups.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(l);
        }

        List<DailyUsageDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<RequestLog>> entry : dailyGroups.entrySet()) {
            List<RequestLog> dayLogs = entry.getValue();
            long requestCount = dayLogs.size();
            long totalTokens = dayLogs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum();
            BigDecimal totalCost = dayLogs.stream()
                    .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(DailyUsageDTO.builder()
                    .date(entry.getKey())
                    .requestCount(requestCount)
                    .totalTokens(totalTokens)
                    .totalCost(totalCost)
                    .build());
        }

        return result;
    }

    @Transactional
    public ModelPrice addModelPrice(ModelPriceCreateRequest request) {
        Long existCount = modelPriceMapper.selectCount(
                new LambdaQueryWrapper<ModelPrice>().eq(ModelPrice::getModelId, request.getModelId())
        );
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型定价已存在");
        }

        ModelPrice modelPrice = new ModelPrice();
        modelPrice.setModelId(request.getModelId());
        modelPrice.setModelName(request.getModelName());
        modelPrice.setInputPrice(request.getInputPrice());
        modelPrice.setOutputPrice(request.getOutputPrice());
        modelPrice.setPriceMultiplier(request.getPriceMultiplier() != null ? request.getPriceMultiplier() : BigDecimal.ONE);
        modelPrice.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        modelPriceMapper.insert(modelPrice);
        log.info("添加模型定价: modelId={}, modelName={}", request.getModelId(), request.getModelName());
        return modelPrice;
    }

    @Transactional
    public ModelPrice updateModelPrice(Long id, ModelPriceCreateRequest request) {
        ModelPrice modelPrice = modelPriceMapper.selectById(id);
        if (modelPrice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型定价不存在");
        }

        if (request.getModelId() != null) {
            modelPrice.setModelId(request.getModelId());
        }
        if (request.getModelName() != null) {
            modelPrice.setModelName(request.getModelName());
        }
        if (request.getInputPrice() != null) {
            modelPrice.setInputPrice(request.getInputPrice());
        }
        if (request.getOutputPrice() != null) {
            modelPrice.setOutputPrice(request.getOutputPrice());
        }
        if (request.getPriceMultiplier() != null) {
            modelPrice.setPriceMultiplier(request.getPriceMultiplier());
        }
        if (request.getStatus() != null) {
            modelPrice.setStatus(request.getStatus());
        }

        modelPriceMapper.updateById(modelPrice);
        log.info("更新模型定价: id={}, modelId={}", id, modelPrice.getModelId());
        return modelPrice;
    }

    public IPage<ModelPrice> listModelPrices(int page, int size) {
        Page<ModelPrice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ModelPrice> wrapper = new LambdaQueryWrapper<ModelPrice>()
                .orderByAsc(ModelPrice::getModelId);
        return modelPriceMapper.selectPage(pageParam, wrapper);
    }

    @Transactional
    public void deleteModelPrice(Long id) {
        ModelPrice modelPrice = modelPriceMapper.selectById(id);
        if (modelPrice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型定价不存在");
        }
        modelPriceMapper.deleteById(id);
        log.info("删除模型定价: id={}, modelId={}", id, modelPrice.getModelId());
    }

    @Transactional
    public void toggleChannel(Long channelId, boolean enabled) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通道不存在");
        }
        channel.setStatus(enabled ? 1 : 0);
        channelMapper.updateById(channel);
        log.info("切换通道状态: channelId={}, enabled={}", channelId, enabled);
    }

    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("重置用户密码: userId={}", userId);
    }
}
