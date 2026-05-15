package com.airelay.relay.handler;

import com.airelay.relay.dto.TokenUsage;
import com.airelay.relay.service.TokenCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHandler {

    private final TokenCountService tokenCountService;

    public Flux<String> handleStreamingResponse(WebClient.ResponseSpec responseSpec) {
        List<String> chunks = new ArrayList<>();

        return responseSpec.bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    synchronized (chunks) {
                        chunks.add(chunk);
                    }
                })
                .doOnComplete(() -> log.debug("SSE stream completed, total chunks: {}", chunks.size()))
                .doOnError(e -> log.error("SSE stream error: {}", e.getMessage()));
    }

    public TokenUsage extractUsageFromChunks(List<String> chunks) {
        return tokenCountService.extractTokenUsageFromSseChunks(chunks);
    }
}
