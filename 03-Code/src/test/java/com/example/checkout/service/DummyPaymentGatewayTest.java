package com.example.checkout.service;

import com.example.checkout.config.TestCardProperties;
import com.example.checkout.config.TestCardProperties.Card;
import com.example.checkout.config.TestCardProperties.Outcome;
import com.example.checkout.exception.GatewayTimeoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure-Java unit tests — no Spring context. Properties are constructed
 * by hand so each test owns its scenario.
 */
class DummyPaymentGatewayTest {

    /** Mirrors the production YAML so the test reflects real behaviour. */
    private static DummyPaymentGateway gatewayWithDefaults() {
        TestCardProperties props = new TestCardProperties();
        props.setTimeoutThresholdMs(5_000L);
        props.setDefaultDeclineReason("do_not_honor");
        props.setCards(List.of(
                card("4111111111111111", Outcome.APPROVE, null),
                card("4000000000000002", Outcome.DECLINE, "insufficient_funds"),
                card("4000000000000069", Outcome.TIMEOUT, null),
                card("5555555555554444", Outcome.APPROVE, null),
                card("378282246310005",  Outcome.APPROVE, null),
                card("4000000000000341", Outcome.DECLINE, "expired_card"),
                card("4000000000000127", Outcome.DECLINE, "incorrect_cvc"),
                card("4000000000000119", Outcome.DECLINE, "processing_error"),
                card("4100000000000019", Outcome.DECLINE, "fraudulent"),
                card("4242424242424242", Outcome.APPROVE, null),
                card("4000002500003155", Outcome.DECLINE, "requires_authentication"),
                card("6011000000000004", Outcome.APPROVE, null),
                card("4000000000000226", Outcome.TIMEOUT, null),
                card("4000000000009995", Outcome.DECLINE, "stolen_card"),
                card("6105105105105100", Outcome.APPROVE, null),
                card("8000000000000010", Outcome.DECLINE, "expired_card"),
                card("8105105105105100", Outcome.APPROVE, null)
        ));
        return new DummyPaymentGateway(props);
    }

    private static Card card(String number, Outcome outcome, String reason) {
        Card c = new Card();
        c.setNumber(number);
        c.setOutcome(outcome);
        c.setReason(reason);
        return c;
    }

    @Test
    void approveCardReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4111111111111111", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void declineCardReturnsInsufficientFunds() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000002", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.DECLINED);
        assertThat(result.declineReason()).isEqualTo("insufficient_funds");
    }

    @Test
    void unknownCardFallsBackToDefaultDecline() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("9999888877776666", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("do_not_honor");
    }

    @Test
    void declineWithoutExplicitReasonFallsBackToDefault() {
        TestCardProperties props = new TestCardProperties();
        props.setDefaultDeclineReason("custom_default");
        props.setCards(List.of(card("5555555555554444", Outcome.DECLINE, null)));
        DummyPaymentGateway gateway = new DummyPaymentGateway(props);

        var result = gateway.charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("5555555555554444", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("custom_default");
    }

    @Test
    void mastercardApproveReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("5555555555554444", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
    }

    @Test
    void amexApproveReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("378282246310005", "12/29", "1234", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
    }

    @Test
    void expiredCardReturnsExpiredCard() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000341", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("expired_card");
    }

    @Test
    void incorrectCvcCardReturnsIncorrectCvc() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000127", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("incorrect_cvc");
    }

    @Test
    void processingErrorCardReturnsProcessingError() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000119", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("processing_error");
    }

    @Test
    void fraudulentCardReturnsFraudulent() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4100000000000019", "12/29", "123", "X"));

        assertThat(result.declineReason()).isEqualTo("fraudulent");
    }

    @Test
    void alternativeApproveCardReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4242424242424242", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void requires3dsCardReturnsRequiresAuthentication() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000002500003155", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.DECLINED);
        assertThat(result.declineReason()).isEqualTo("requires_authentication");
    }

    @Test
    void discoverApproveReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("6011000000000004", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void altMastercardApproveCardReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("6105105105105100", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void stolenCardReturnsStolenCard() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000009995", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.DECLINED);
        assertThat(result.declineReason()).isEqualTo("stolen_card");
    }

    @Test
    void customBinApproveCardReturnsApproved() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("8105105105105100", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.APPROVED);
        assertThat(result.transactionId()).startsWith("txn-");
    }

    @Test
    void txnLimitedExpiredCardReturnsExpiredCard() {
        var result = gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("8000000000000010", "12/29", "123", "X"));

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentResult.Status.DECLINED);
        assertThat(result.declineReason()).isEqualTo("expired_card");
    }

    /** Slow — runs only in nightly CI. */
    @org.junit.jupiter.api.Disabled("slow — runs in nightly CI")
    @Test
    void timeoutCardThrows() {
        assertThatThrownBy(() -> gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000069", "12/29", "123", "X")))
                .isInstanceOf(GatewayTimeoutException.class);
    }

    /** Slow — runs only in nightly CI. */
    @org.junit.jupiter.api.Disabled("slow — runs in nightly CI")
    @Test
    void alternativeTimeoutCardThrows() {
        assertThatThrownBy(() -> gatewayWithDefaults().charge(BigDecimal.TEN, "INR",
                new PaymentGateway.CardDetails("4000000000000226", "12/29", "123", "X")))
                .isInstanceOf(GatewayTimeoutException.class);
    }
}
