package com.example.checkout.service;

import com.example.checkout.config.TestCardProperties;
import com.example.checkout.config.TestCardProperties.Card;
import com.example.checkout.config.TestCardProperties.Outcome;
import com.example.checkout.exception.GatewayTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic fake gateway whose behaviour is driven entirely by
 * {@code gateway.test-cards.*} in application.yml.
 *
 * <p>Adding a new test scenario no longer requires changing Java — append a
 * card entry to the YAML and the rule applies on next boot.
 */
@Component
public class DummyPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(DummyPaymentGateway.class);

    private final TestCardProperties props;
    /** Pre-indexed for O(1) lookup. */
    private final Map<String, Card> index;

    public DummyPaymentGateway(TestCardProperties props) {
        this.props = props;
        this.index = props.getCards().stream()
                .collect(Collectors.toUnmodifiableMap(Card::getNumber, Function.identity()));
        log.info("DummyPaymentGateway initialised with {} test card(s)", index.size());
    }

    @Override
    public PaymentResult charge(BigDecimal amount, String currency, CardDetails card) {
        Card rule = index.get(card.number());
        if (rule == null) {
            log.debug("Unknown test card → default decline ({})", props.getDefaultDeclineReason());
            return PaymentResult.declined(props.getDefaultDeclineReason());
        }

        return switch (rule.getOutcome()) {
            case APPROVE -> PaymentResult.approved("txn-" + UUID.randomUUID());
            case DECLINE -> PaymentResult.declined(
                    rule.getReason() != null ? rule.getReason() : props.getDefaultDeclineReason());
            case TIMEOUT -> {
                sleep(props.getTimeoutThresholdMs() + 1_000);
                throw new GatewayTimeoutException();
            }
        };
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
