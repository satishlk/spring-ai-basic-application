package com.example.orchestrator.controller.dto;

public record TriggerResponse(String status, String correlationId, String message) {
    public static TriggerResponse accepted(String correlationId) {
        return new TriggerResponse("accepted", correlationId,
                "Event queued for async agent execution.");
    }
    public static TriggerResponse rejected(String correlationId, String reason) {
        return new TriggerResponse("rejected", correlationId, reason);
    }
}
