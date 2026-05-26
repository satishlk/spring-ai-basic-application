package com.example.orchestrator.controller;

import com.example.orchestrator.controller.dto.PipelineEventPayload;
import com.example.orchestrator.controller.dto.TriggerResponse;
import com.example.orchestrator.service.PipelineEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Webhook ingress. Accepts a {@link PipelineEventPayload}, mints a
 * correlationId, hands the work off to a virtual-thread executor, and
 * replies HTTP 202 immediately. CI systems never block waiting for
 * Claude to finish.
 *
 * <p>Why this is the entire controller layer:
 * <ul>
 *   <li>Validation: {@code @Valid} + jakarta annotations on the DTO.</li>
 *   <li>Async: handoff to virtual-thread executor — Servlet thread returns instantly.</li>
 *   <li>Observability: correlationId in MDC for every log line downstream.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1")
public class TriggerController {

    private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

    private final PipelineEventService service;
    private final ExecutorService executor;

    public TriggerController(PipelineEventService service,
                             @Qualifier("agentTaskExecutor") ExecutorService executor) {
        this.service  = service;
        this.executor = executor;
    }

    @PostMapping(value = "/trigger",
                 consumes = "application/json",
                 produces = "application/json")
    public ResponseEntity<TriggerResponse> trigger(@Valid @RequestBody PipelineEventPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("eventType",     payload.eventType());

        try {
            log.info("Accepted event type={} task={} chars; handing off to executor",
                     payload.eventType(), payload.task().length());

            // Copy MDC into the virtual thread (Spring doesn't propagate automatically here)
            String corrCopy = correlationId;
            String typeCopy = payload.eventType();
            executor.submit(() -> {
                MDC.put("correlationId", corrCopy);
                MDC.put("eventType",     typeCopy);
                try {
                    service.handle(payload, corrCopy);
                } catch (Exception e) {
                    log.error("Agent execution failed: {}", e.getMessage(), e);
                } finally {
                    MDC.clear();
                }
            });

            return ResponseEntity.accepted().body(TriggerResponse.accepted(correlationId));
        } finally {
            MDC.clear();
        }
    }
}
