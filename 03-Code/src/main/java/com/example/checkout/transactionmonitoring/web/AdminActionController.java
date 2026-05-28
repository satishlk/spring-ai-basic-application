package com.example.checkout.transactionmonitoring.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for administrative transaction management actions.
 * Supports refunds (full/partial), voids, and dispute flagging.
 * All actions require appropriate role-based authorization and are audited.
 */
@RestController
@RequestMapping("/transactions/{id}")
public class AdminActionController {

    // AuditLogService will be injected by Spring; implementation in follow-up
    // TransactionRepository will be injected by Spring; implementation in follow-up
    // PaymentGatewayClient will be injected by Spring; implementation in follow-up

    /**
     * Issue a refund for a completed transaction.
     * Supports both full and partial refunds.
     * Validates that refund amount does not exceed (original amount - previous refunds).
     *
     * @param id Transaction ID
     * @param request Refund details (type: FULL or PARTIAL, amount for partial)
     * @return Refund confirmation with updated transaction status
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable String id,
            @RequestBody RefundRequest request) {
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
     * @param request Dispute details (reason)
     * @return Dispute confirmation with updated transaction status
     */
    @PostMapping("/dispute")
    public ResponseEntity<DisputeResponse> dispute(
            @PathVariable String id,
            @RequestBody DisputeRequest request) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Request object for refund action.
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
     * Response object for refund action.
     */
    public record RefundResponse(
            String transactionId,
            String status,
            BigDecimal refundedAmount,
            BigDecimal remainingAmount,
            String message
    ) {}

    /**
     * Response object for void action.
     */
    public record VoidResponse(
            String transactionId,
            String status,
            String message
    ) {}

    /**
     * Request object for dispute action.
     */
    public record DisputeRequest(
            String reason
    ) {}

    /**
     * Response object for dispute action.
     */
    public record DisputeResponse(
            String transactionId,
            String status,
            String disputeId,
            String message
    ) {}
}
