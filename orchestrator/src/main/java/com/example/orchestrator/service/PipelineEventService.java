package com.example.orchestrator.service;

import com.example.orchestrator.audit.AuditService;
import com.example.orchestrator.config.ConcurrencyGate;
import com.example.orchestrator.config.McpProperties;
import com.example.orchestrator.controller.dto.PipelineEventPayload;
import com.example.orchestrator.prompt.AgentProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Top-level orchestrator. Called from the controller on a virtual thread.
 *
 * <p>Order of operations:
 * <ol>
 *   <li>Check the kill switch ({@link McpProperties#isEnabled()}) — if off,
 *       log + return immediately. No Claude call, no MCP call.</li>
 *   <li>Resolve the agent profile from the event type.</li>
 *   <li>Acquire a concurrency permit (Semaphore-bounded). If the gate is
 *       full beyond the acquire timeout, drop the event with a warning.</li>
 *   <li>Hand off to {@link ClaudeAgentRunner}.</li>
 *   <li>Release the permit in a finally block — even on exception.</li>
 * </ol>
 *
 * <p>The {@link ClaudeAgentRunner} dependency is wrapped in
 * {@link ObjectProvider} so that this service is constructible even when
 * the kill switch is OFF and the MCP-dependent beans are absent.
 */
@Service
public class PipelineEventService {

    private static final Logger log = LoggerFactory.getLogger(PipelineEventService.class);

    private final McpProperties mcpProps;
    private final AgentRouter   router;
    private final ConcurrencyGate gate;
    private final ObjectProvider<ClaudeAgentRunner> runnerProvider;
    private final AuditService audit;

    public PipelineEventService(McpProperties mcpProps,
                                AgentRouter router,
                                ConcurrencyGate gate,
                                ObjectProvider<ClaudeAgentRunner> runnerProvider,
                                AuditService audit) {
        this.mcpProps       = mcpProps;
        this.router         = router;
        this.gate           = gate;
        this.runnerProvider = runnerProvider;
        this.audit          = audit;
    }

    public void handle(PipelineEventPayload payload, String correlationId) {
        audit.accept(correlationId, payload.eventType(), payload.task());

        if (!mcpProps.isEnabled()) {
            log.warn("Agent execution disabled by mcp.github.enabled=false; skipping event {}",
                     correlationId);
            audit.markRejected(correlationId, "mcp.github.enabled=false");
            return;
        }

        AgentProfile profile;
        try {
            profile = router.resolve(payload.eventType());
        } catch (RuntimeException e) {
            log.error("Cannot route event: {}", e.getMessage());
            audit.markRejected(correlationId, "Unknown eventType: " + payload.eventType());
            return;
        }
        MDC.put("agent", profile.name());

        ClaudeAgentRunner runner = runnerProvider.getIfAvailable();
        if (runner == null) {
            log.error("ClaudeAgentRunner not available — MCP bean wiring failed; check boot logs");
            audit.markFailed(correlationId, "ClaudeAgentRunner bean unavailable");
            return;
        }

        boolean acquired = false;
        try {
            acquired = gate.tryAcquire();
            if (!acquired) {
                log.warn("Concurrency gate timed out (available permits={}); dropping event",
                         gate.available());
                audit.markRejected(correlationId, "Concurrency gate timed out");
                return;
            }
            log.info("Permit acquired (remaining={}); invoking agent", gate.available());
            audit.markRunning(correlationId);

            String summary = runner.run(profile, payload.task());
            log.info("Agent run complete — final summary follows ({}chars):\n{}",
                     summary.length(), summary);
            audit.markCompleted(correlationId, summary);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while awaiting concurrency permit");
            audit.markFailed(correlationId, "Interrupted");
        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage(), e);
            audit.markFailed(correlationId, e.getMessage());
        } finally {
            if (acquired) gate.release();
        }
    }
}
