package com.example.checkout.service;

import java.math.BigDecimal;

/**
 * The single seam where a real provider (Stripe, Razorpay, etc.) plugs in.
 * Stage 2 §5 — the only place outside-the-system money movement lives.
 */
public interface PaymentGateway {

    PaymentResult charge(BigDecimal amount, String currency, CardDetails card);

    record CardDetails(String number, String expiry, String cvv, String holder) { }

    record PaymentResult(Status status, String transactionId, String declineReason) {
        public enum Status { APPROVED, DECLINED }

        public static PaymentResult approved(String txn) {
            return new PaymentResult(Status.APPROVED, txn, null);
        }

        public static PaymentResult declined(String reason) {
            return new PaymentResult(Status.DECLINED, null, reason);
        }
    }
}
