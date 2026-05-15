package com.airelay.relay.strategy;

public enum LoadBalanceStrategy {

    PRIORITY,
    WEIGHTED_RANDOM,
    ROUND_ROBIN,
    LEAST_LATENCY
}
