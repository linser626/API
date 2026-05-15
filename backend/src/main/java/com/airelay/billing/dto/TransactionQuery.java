package com.airelay.billing.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionQuery {

    private String type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer page = 1;

    private Integer size = 10;
}
