package com.example.checkout.exception;

public class GatewayTimeoutException extends CheckoutException {
    public GatewayTimeoutException() {
        super("GATEWAY_TIMEOUT", "Payment gateway timed out");
    }
}
