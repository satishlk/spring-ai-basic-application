package com.example.checkout.service;

import com.example.checkout.exception.GatewayTimeoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DummyPaymentGatewayTest {

    private final DummyPaymentGateway gateway = new DummyPaymentGateway();

    @Test
    void approveCardReturnsApproved() {
        var result = gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4111111111111111", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void mastercard2SeriesApproveCardReturnsApproved() {
        var result = gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("2205105105105100", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void declineCardReturnsInsufficientFunds() {
        var result = gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000002", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.DECLINED);
        assertThat(result.declineReason()).isEqualTo("insufficient_funds");
    }

    @Test
    void unknownCardIsDoNotHonor() {
        var result = gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("9999888877776666", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("do_not_honor");
    }

    /**
     * The timeout test is *not* run by default because it sleeps >5 s.
     * Enable in CI nightly job. Kept here to show how to test the path.
     */
    @org.junit.jupiter.api.Disabled("slow — runs in nightly CI")
    @Test
    void timeoutCardThrows() {
        assertThatThrownBy(() -> gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000069", "12/29", "123", "X")))
                .isInstanceOf(GatewayTimeoutException.class);
    }
}
