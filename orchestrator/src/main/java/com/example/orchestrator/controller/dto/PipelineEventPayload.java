package com.example.orchestrator.controller.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Webhook body posted by CI (or a curl smoke test). Two flavours of event
 * are supported today; the {@code eventType} picks the agent profile.
 *
 * <p>Examples:
 * <pre>{@code
 * // CI build went red — trigger the failure-fixer agent
 * {
 *   "eventType": "ci-failure-fixer",
 *   "task": "Pipeline 'verify' failed at step 'mvn test'. Stack trace:\n  …",
 *   "metadata": { "ciRunId": "9876", "sourceBranch": "feat/payment-rewrite" }
 * }
 *
 * // Manual / scripted card addition
 * {
 *   "eventType": "test-card-curator-remote",
 *   "task": "add a card 4485105105105100 that approves",
 *   "metadata": { "requestedBy": "satish" }
 * }
 * }</pre>
 */
public record PipelineEventPayload(

    /** Selects an agent profile from {@code agents.profiles.*}. */
    @NotBlank String eventType,

    /** The free-text task for the agent. Becomes the user message to Claude. */
    @NotBlank String task,

    /** Optional context that the agent may need (CI URL, requestor, etc.). */
    Map<String, Object> metadata
) {}
