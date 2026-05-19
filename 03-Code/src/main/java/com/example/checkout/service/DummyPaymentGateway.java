package com.example.checkout.service;

import com.example.checkout.exception.GatewayTimeoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic fake gateway driven by the test-card table in Stage 2 §5.
 * Replace this @Component with a Stripe-backed one and nothing else changes.
 */
@Component
public class DummyPaymentGateway implements PaymentGateway {

    static final String CARD_APPROVE      = "4111111111111111";
    static final String CARD_APPROVE_MC2  = "2205105105105100";
    static final String CARD_DECLINE      = "4000000000000002";
    static final String CARD_TIMEOUT      = "4000000000000069";
    static final long   TIMEOUT_THRESHOLD = 5_000L;

    @Override
    public PaymentResult charge(BigDecimal amount, String currency, CardDetails card) {
        if (CARD_TIMEOUT.equals(card.number())) {
            sleep(TIMEOUT_THRESHOLD + 1_000);
            throw new GatewayTimeoutException();
        }
        if (CARD_APPROVE.equals(card.number()) || CARD_APPROVE_MC2.equals(card.number())) {
            return PaymentResult.approved("txn-" + UUID.randomUUID());
        }
        if (CARD_DECLINE.equals(card.number())) {
            return PaymentResult.declined("insufficient_funds");
        }
        return PaymentResult.declined("do_not_honor");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
