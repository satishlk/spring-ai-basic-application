package com.example.checkout.transactionmonitoring.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for administrative actions on transactions.
 * Supports refunds (full/partial), voids, and dispute flagging as defined in PRD section 2.
 * All actions require authentication and role-based authorization.
 * All actions are idempotent and create audit log entries.
 */
@RestController
@RequestMapping("/transactions/{id}")
public class AdminActionController {

    // Injected by Spring; impl in follow-up
    // private final RefundService refundService;
    // private final VoidService voidService;
    // private final DisputeService disputeService;
    // private final AuditLogService auditLogService;

    /**
     * Issue a refund for a completed transaction.
     * Supports both full and partial refunds.
     * Validates that refund amount does not exceed (original_amount - sum_of_previous_refunds).
     *
     * @param id Transaction ID
     * @param request Refund request containing type (FULL/PARTIAL) and amount
     * @return Refund confirmation with updated transaction status
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable String id,
            @RequestBody RefundRequest request) {
        
        // Validate request
        if (request.type() == null) {
            throw new IllegalArgumentException("Refund type is required");
        }
        
        if (request.type() == RefundType.PARTIAL && request.amount() == null) {
            throw new IllegalArgumentException("Amount is required for partial refunds");
        }
        
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Void a pending or authorized transaction before settlement.
     * Only permitted for transactions in pending or authorized status.
     *
     * @param id Transaction ID
     * @return Void confirmation with updated transaction status
     */
    @PostMapping("/void")
    public ResponseEntity<VoidResponse> voidTransaction(@PathVariable String id) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Flag a transaction as disputed and trigger dispute resolution workflow.
     *
     * @param id Transaction ID
     * @param request Dispute request containing reason
     * @return Dispute confirmation with updated transaction status
     */
    @PostMapping("/dispute")
    public ResponseEntity<DisputeResponse> dispute(
            @PathVariable String id,
            @RequestBody DisputeRequest request) {
        
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }
        
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Refund request payload.
     *
     * @param type FULL or PARTIAL
     * @param amount Required for PARTIAL refunds; must be ≤ (original_amount - sum_of_previous_refunds)
     */
    public record RefundRequest(
        RefundType type,
        BigDecimal amount
    ) {}

    /**
     * Refund type enumeration.
     */
    public enum RefundType {
        FULL,
        PARTIAL
    }

    /**
     * Refund response payload.
     *
     * @param transactionId Transaction ID
     * @param refundAmount Amount refunded
     * @param newStatus Updated transaction status (refunded or partially_refunded)
     * @param refundId Unique refund identifier
     */
    public record RefundResponse(
        String transactionId,
        BigDecimal refundAmount,
        String newStatus,
        String refundId
    ) {}

    /**
     * Void response payload.
     *
     * @param transactionId Transaction ID
     * @param newStatus Updated transaction status (voided)
     */
    public record VoidResponse(
        String transactionId,
        String newStatus
    ) {}

    /**
     * Dispute request payload.
     *
     * @param reason Reason for dispute (free text, min 10 chars recommended)
     */
    public record DisputeRequest(
        String reason
    ) {}

    /**
     * Dispute response payload.
     *
     * @param transactionId Transaction ID
     * @param newStatus Updated transaction status (disputed)
     * @param disputeId Unique dispute identifier
     */
    public record DisputeResponse(
        String transactionId,
        String newStatus,
        String disputeId
    ) {}
}
