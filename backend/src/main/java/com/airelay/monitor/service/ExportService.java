package com.airelay.monitor.service;

import com.airelay.billing.entity.BalanceTransaction;
import com.airelay.billing.mapper.BalanceTransactionMapper;
import com.airelay.relay.entity.RequestLog;
import com.airelay.relay.mapper.RequestLogMapper;
import com.airelay.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final RequestLogMapper requestLogMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;

    private static final int MAX_EXPORT_ROWS = 100000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public byte[] exportUsage(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .eq(RequestLog::getUserId, userId);

        if (startTime != null) {
            wrapper.ge(RequestLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(RequestLog::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(RequestLog::getCreatedAt).last("LIMIT " + MAX_EXPORT_ROWS);

        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("ID,模型,请求类型,输入Token,输出Token,总Token,费用,延迟(ms),状态,错误信息,IP地址,创建时间\n");

        for (RequestLog logEntry : logs) {
            sb.append(escapeCsv(String.valueOf(logEntry.getId()))).append(',');
            sb.append(escapeCsv(logEntry.getModel())).append(',');
            sb.append(escapeCsv(logEntry.getRequestType())).append(',');
            sb.append(logEntry.getPromptTokens() != null ? logEntry.getPromptTokens() : 0).append(',');
            sb.append(logEntry.getCompletionTokens() != null ? logEntry.getCompletionTokens() : 0).append(',');
            sb.append(logEntry.getTotalTokens() != null ? logEntry.getTotalTokens() : 0).append(',');
            sb.append(logEntry.getCost() != null ? logEntry.getCost().toPlainString() : "0").append(',');
            sb.append(logEntry.getLatencyMs() != null ? logEntry.getLatencyMs() : 0).append(',');
            sb.append(escapeCsv(logEntry.getStatus())).append(',');
            sb.append(escapeCsv(logEntry.getErrorMessage())).append(',');
            sb.append(escapeCsv(logEntry.getIpAddress())).append(',');
            sb.append(escapeCsv(logEntry.getCreatedAt() != null ? logEntry.getCreatedAt().format(DATE_FORMATTER) : "")).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportTransactions(Long userId, String type, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<BalanceTransaction> wrapper = new LambdaQueryWrapper<BalanceTransaction>()
                .eq(BalanceTransaction::getUserId, userId);

        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(BalanceTransaction::getType, type);
        }
        if (startTime != null) {
            wrapper.ge(BalanceTransaction::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(BalanceTransaction::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(BalanceTransaction::getCreatedAt).last("LIMIT " + MAX_EXPORT_ROWS);

        List<BalanceTransaction> transactions = balanceTransactionMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("ID,类型,金额,变动前余额,变动后余额,描述,订单ID,创建时间\n");

        for (BalanceTransaction tx : transactions) {
            sb.append(tx.getId()).append(',');
            sb.append(escapeCsv(tx.getType())).append(',');
            sb.append(tx.getAmount() != null ? tx.getAmount().toPlainString() : "0").append(',');
            sb.append(tx.getBalanceBefore() != null ? tx.getBalanceBefore().toPlainString() : "0").append(',');
            sb.append(tx.getBalanceAfter() != null ? tx.getBalanceAfter().toPlainString() : "0").append(',');
            sb.append(escapeCsv(tx.getDescription())).append(',');
            sb.append(tx.getOrderId() != null ? tx.getOrderId() : "").append(',');
            sb.append(escapeCsv(tx.getCreatedAt() != null ? tx.getCreatedAt().format(DATE_FORMATTER) : "")).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportLogs(Long userId, String model, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(RequestLog::getUserId, userId);
        }
        if (model != null && !model.trim().isEmpty()) {
            wrapper.eq(RequestLog::getModel, model);
        }
        if (startTime != null) {
            wrapper.ge(RequestLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(RequestLog::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(RequestLog::getCreatedAt).last("LIMIT " + MAX_EXPORT_ROWS);

        List<RequestLog> logs = requestLogMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("ID,用户ID,API密钥ID,通道ID,模型,请求类型,输入Token,输出Token,总Token,费用,延迟(ms),状态,错误信息,IP地址,创建时间\n");

        for (RequestLog logEntry : logs) {
            sb.append(logEntry.getId()).append(',');
            sb.append(logEntry.getUserId()).append(',');
            sb.append(logEntry.getApiKeyId() != null ? logEntry.getApiKeyId() : "").append(',');
            sb.append(logEntry.getChannelId() != null ? logEntry.getChannelId() : "").append(',');
            sb.append(escapeCsv(logEntry.getModel())).append(',');
            sb.append(escapeCsv(logEntry.getRequestType())).append(',');
            sb.append(logEntry.getPromptTokens() != null ? logEntry.getPromptTokens() : 0).append(',');
            sb.append(logEntry.getCompletionTokens() != null ? logEntry.getCompletionTokens() : 0).append(',');
            sb.append(logEntry.getTotalTokens() != null ? logEntry.getTotalTokens() : 0).append(',');
            sb.append(logEntry.getCost() != null ? logEntry.getCost().toPlainString() : "0").append(',');
            sb.append(logEntry.getLatencyMs() != null ? logEntry.getLatencyMs() : 0).append(',');
            sb.append(escapeCsv(logEntry.getStatus())).append(',');
            sb.append(escapeCsv(logEntry.getErrorMessage())).append(',');
            sb.append(escapeCsv(logEntry.getIpAddress())).append(',');
            sb.append(escapeCsv(logEntry.getCreatedAt() != null ? logEntry.getCreatedAt().format(DATE_FORMATTER) : "")).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String generateFilename(String type) {
        return "export_" + type + "_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".csv";
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
