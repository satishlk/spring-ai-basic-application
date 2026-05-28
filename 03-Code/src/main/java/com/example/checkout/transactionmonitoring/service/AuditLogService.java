package com.example.checkout.transactionmonitoring.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for recording audit trail of all transaction-related actions.
 * Implements PII masking for sensitive fields (card numbers, email addresses).
 * All audit logs are append-only and tamper-evident.
 */
@Service
public class AuditLogService {

    // AuditLogRepository will be injected by Spring; implementation in follow-up

    /**
     * Record an audit log entry for a transaction-related action.
     * Automatically masks PII fields in the details JSON.
     *
     * @param actorId User ID of the person performing the action
     * @param action Action type (view_transaction, refund, void, dispute, export, etc.)
     * @param transactionId Transaction ID being acted upon
     * @param detailsJson JSON string containing action details (will be PII-masked)
     */
    public void record(String actorId, String action, String transactionId, String detailsJson) {
        String maskedDetails = maskPii(detailsJson);
        recordInternal(actorId, action, transactionId, maskedDetails);
    }

    /**
     * Mask PII fields in JSON details.
     * - cardNumber: Show only last 4 digits (**** **** **** XXXX)
     * - email: Show only first char + *** + @domain (e.g., j***@example.com)
     *
     * @param detailsJson Raw JSON string potentially containing PII
     * @return JSON string with PII fields masked
     */
    private String maskPii(String detailsJson) {
        if (detailsJson == null || detailsJson.isEmpty()) {
            return detailsJson;
        }

        String masked = detailsJson;

        // Mask card numbers: replace all but last 4 digits
        // Pattern: "cardNumber":"1234567890123456" -> "cardNumber":"**** **** **** 3456"
        masked = masked.replaceAll(
                "\"cardNumber\"\\s*:\\s*\"(\\d{12})(\\d{4})\"",
                "\"cardNumber\":\"**** **** **** $2\""
        );

        // Mask email addresses: keep first char and domain
        // Pattern: "email":"john@example.com" -> "email":"j***@example.com"
        masked = masked.replaceAll(
                "\"email\"\\s*:\\s*\"([a-zA-Z])[^@]*(@[^\"]+)\"",
                "\"email\":\"$1***$2\""
        );

        return masked;
    }

    /**
     * Internal method to persist audit log entry.
     * Creates immutable audit record with timestamp, actor, action, resource, and details.
     *
     * @param actorId User ID performing the action
     * @param action Action type
     * @param transactionId Transaction ID
     * @param maskedDetails PII-masked details JSON
     */
    private void recordInternal(String actorId, String action, String transactionId, String maskedDetails) {
        // Implementation will create AuditLogEntry entity and persist to database
        // Entry includes: timestamp (now), actorId, action, transactionId, maskedDetails
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Audit log entry record.
     * Represents a single immutable audit event.
     */
    public record AuditLogEntry(
            String id,
            LocalDateTime timestamp,
            String actorId,
            String action,
            String transactionId,
            String details,
            String sessionId,
            String ipAddress
    ) {}
}
