package com.example.checkout.exception;

public class CheckoutException extends RuntimeException {
    private final String code;

    public CheckoutException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
