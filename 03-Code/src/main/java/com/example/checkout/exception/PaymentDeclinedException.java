package com.example.checkout.exception;

public class PaymentDeclinedException extends CheckoutException {
    private final String reason;

    public PaymentDeclinedException(String reason) {
        super("DECLINED", "Payment declined: " + reason);
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
