package com.example.checkout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed binding for the {@code gateway.test-cards.*} section of
 * application.yml. Spring populates this class automatically because of
 * {@link org.springframework.boot.context.properties.ConfigurationPropertiesScan}
 * on {@link com.example.checkout.CheckoutApplication}.
 *
 * <p>Why a typed class rather than reading {@code @Value} strings?
 * <ul>
 *   <li>Validation: Spring fails fast at boot if YAML structure is wrong.</li>
 *   <li>Refactor-safe: renaming a field shows up as a compile error.</li>
 *   <li>Testable: easy to construct in unit tests without a Spring context.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "gateway.test-cards")
public class TestCardProperties {

    /** How long the gateway "sleeps" before throwing on a timeout card. */
    private long timeoutThresholdMs = 5_000L;

    /** Reason returned when the card number does not match any rule. */
    private String defaultDeclineReason = "do_not_honor";

    /** The card table — ordering does not matter; lookup is by card number. */
    private List<Card> cards = new ArrayList<>();

    public long getTimeoutThresholdMs() { return timeoutThresholdMs; }
    public void setTimeoutThresholdMs(long timeoutThresholdMs) { this.timeoutThresholdMs = timeoutThresholdMs; }

    public String getDefaultDeclineReason() { return defaultDeclineReason; }
    public void setDefaultDeclineReason(String defaultDeclineReason) { this.defaultDeclineReason = defaultDeclineReason; }

    public List<Card> getCards() { return cards; }
    public void setCards(List<Card> cards) { this.cards = cards; }

    /** A single row of the test-card table. */
    public static class Card {
        private String number;
        private Outcome outcome;
        /** Only used when outcome == DECLINE. */
        private String reason;
        /** Human-readable label, shown in logs/tests. */
        private String label;

        public String getNumber()  { return number; }
        public void setNumber(String number) { this.number = number; }

        public Outcome getOutcome() { return outcome; }
        public void setOutcome(Outcome outcome) { this.outcome = outcome; }

        public String getReason()  { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getLabel()   { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public enum Outcome {
        APPROVE,
        DECLINE,
        TIMEOUT
    }
}
