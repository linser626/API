package com.airelay.monitor.service;

import com.airelay.channel.entity.Channel;
import com.airelay.channel.mapper.ChannelMapper;
import com.airelay.monitor.dto.ChannelStatsDTO;
import com.airelay.monitor.dto.DailyUsageDTO;
import com.airelay.monitor.dto.DashboardStatsDTO;
import com.airelay.monitor.dto.ModelUsageDTO;
import com.airelay.monitor.dto.UsageStatsDTO;
import com.airelay.relay.entity.RequestLog;
import com.airelay.relay.mapper.RequestLogMapper;
import com.airelay.user.mapper.SubscriptionMapper;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final RequestLogMapper requestLogMapper;
    private final ChannelMapper channelMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;

    public UsageStatsDTO getUserUsageStats(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .eq(RequestLog::getUserId, userId);
        if (startTime != null) {
            wrapper.ge(RequestLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(RequestLog::getCreatedAt, endTime);
        }

        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        long totalRequests = logs.size();
        long successRequests = logs.stream().filter(l -> "success".equals(l.getStatus())).count();
        long failedRequests = totalRequests - successRequests;
        long totalTokens = logs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum();
        long promptTokens = logs.stream().mapToLong(l -> l.getPromptTokens() != null ? l.getPromptTokens() : 0).sum();
        long completionTokens = logs.stream().mapToLong(l -> l.getCompletionTokens() != null ? l.getCompletionTokens() : 0).sum();
        BigDecimal totalCost = logs.stream()
                .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgLatencyMs = totalRequests > 0
                ? logs.stream().mapToLong(l -> l.getLatencyMs() != null ? l.getLatencyMs() : 0).average().orElse(0.0)
                : 0.0;

        return UsageStatsDTO.builder()
                .totalRequests(totalRequests)
                .successRequests(successRequests)
                .failedRequests(failedRequests)
                .totalTokens(totalTokens)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalCost(totalCost)
                .avgLatencyMs(avgLatencyMs)
                .build();
    }

    public List<ModelUsageDTO> getUserModelUsage(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .eq(RequestLog::getUserId, userId);
        if (startTime != null) {
            wrapper.ge(RequestLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(RequestLog::getCreatedAt, endTime);
        }

        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        Map<String, List<RequestLog>> modelGroups = new LinkedHashMap<>();
        for (RequestLog l : logs) {
            modelGroups.computeIfAbsent(l.getModel(), k -> new ArrayList<>()).add(l);
        }

        List<ModelUsageDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<RequestLog>> entry : modelGroups.entrySet()) {
            List<RequestLog> modelLogs = entry.getValue();
            long requestCount = modelLogs.size();
            long totalTokens = modelLogs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum();
            BigDecimal totalCost = modelLogs.stream()
                    .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double avgLatencyMs = modelLogs.stream()
                    .mapToLong(l -> l.getLatencyMs() != null ? l.getLatencyMs() : 0)
                    .average().orElse(0.0);

            result.add(ModelUsageDTO.builder()
                    .model(entry.getKey())
                    .requestCount(requestCount)
                    .totalTokens(totalTokens)
                    .totalCost(totalCost)
                    .avgLatencyMs(avgLatencyMs)
                    .build());
        }

        return result;
    }

    public List<DailyUsageDTO> getUserDailyUsage(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }

        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .eq(RequestLog::getUserId, userId)
                .ge(RequestLog::getCreatedAt, startTime)
                .le(RequestLog::getCreatedAt, endTime);

        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        Map<String, List<RequestLog>> dailyGroups = new LinkedHashMap<>();
        LocalDate start = startTime.toLocalDate();
        LocalDate end = endTime.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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

    public List<ChannelStatsDTO> getChannelStats() {
        List<Channel> channels = channelMapper.selectList(
                new LambdaQueryWrapper<Channel>().eq(Channel::getStatus, 1)
        );

        List<ChannelStatsDTO> result = new ArrayList<>();
        for (Channel channel : channels) {
            long totalRequests = channel.getTotalRequests() != null ? channel.getTotalRequests() : 0L;
            long failedRequests = channel.getFailedRequests() != null ? channel.getFailedRequests() : 0L;
            BigDecimal successRate = totalRequests > 0
                    ? BigDecimal.valueOf(totalRequests - failedRequests)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalRequests), 2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100.00);
            double avgLatencyMs = channel.getResponseTimeMs() != null ? channel.getResponseTimeMs().doubleValue() : 0.0;

            result.add(ChannelStatsDTO.builder()
                    .channelId(channel.getId())
                    .channelName(channel.getName())
                    .channelType(channel.getType())
                    .totalRequests(totalRequests)
                    .failedRequests(failedRequests)
                    .successRate(successRate)
                    .avgLatencyMs(avgLatencyMs)
                    .build());
        }

        return result;
    }

    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<com.airelay.user.entity.User>()
                        .eq(com.airelay.user.entity.User::getStatus, 1)
        );

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        long activeUsers = requestLogMapper.selectCount(
                new LambdaQueryWrapper<RequestLog>()
                        .ge(RequestLog::getCreatedAt, LocalDate.now().minusDays(7).atStartOfDay())
        );

        List<RequestLog> todayLogs = requestLogMapper.selectList(
                new LambdaQueryWrapper<RequestLog>()
                        .ge(RequestLog::getCreatedAt, todayStart)
                        .le(RequestLog::getCreatedAt, todayEnd)
        );

        long todayRequests = todayLogs.size();
        long todayTokens = todayLogs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum();
        BigDecimal todayRevenue = todayLogs.stream()
                .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RequestLog> allLogs = requestLogMapper.selectList(null);
        BigDecimal totalRevenue = allLogs.stream()
                .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeChannels = channelMapper.selectCount(
                new LambdaQueryWrapper<Channel>().eq(Channel::getStatus, 1)
        );

        long activeSubscriptions = subscriptionMapper.selectCount(
                new LambdaQueryWrapper<com.airelay.user.entity.Subscription>()
                        .eq(com.airelay.user.entity.Subscription::getStatus, "active")
        );

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .todayRequests(todayRequests)
                .todayTokens(todayTokens)
                .todayRevenue(todayRevenue)
                .totalRevenue(totalRevenue)
                .activeChannels(activeChannels)
                .activeSubscriptions(activeSubscriptions)
                .build();
    }

    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        long totalChannels = channelMapper.selectCount(null);
        long activeChannels = channelMapper.selectCount(
                new LambdaQueryWrapper<Channel>().eq(Channel::getStatus, 1)
        );
        long totalUsers = userMapper.selectCount(null);
        long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<com.airelay.user.entity.User>()
                        .eq(com.airelay.user.entity.User::getStatus, 1)
        );

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = requestLogMapper.selectCount(
                new LambdaQueryWrapper<RequestLog>().ge(RequestLog::getCreatedAt, oneHourAgo)
        );
        long recentErrors = requestLogMapper.selectCount(
                new LambdaQueryWrapper<RequestLog>()
                        .ge(RequestLog::getCreatedAt, oneHourAgo)
                        .ne(RequestLog::getStatus, "success")
        );

        overview.put("totalChannels", totalChannels);
        overview.put("activeChannels", activeChannels);
        overview.put("totalUsers", totalUsers);
        overview.put("activeUsers", activeUsers);
        overview.put("recentRequestsLastHour", recentRequests);
        overview.put("recentErrorsLastHour", recentErrors);
        overview.put("systemStatus", recentErrors > recentRequests * 0.5 ? "warning" : "healthy");

        return overview;
    }

    public List<RequestLog> getRecentErrors(int limit) {
        if (limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100;
        }

        return requestLogMapper.selectList(
                new LambdaQueryWrapper<RequestLog>()
                        .ne(RequestLog::getStatus, "success")
                        .orderByDesc(RequestLog::getCreatedAt)
                        .last("LIMIT " + limit)
        );
    }
}
