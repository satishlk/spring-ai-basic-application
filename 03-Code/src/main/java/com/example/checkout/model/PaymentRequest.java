package com.example.checkout.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO accepted by POST /api/checkout/pay.
 * Validation annotations enforce FR-2 (payment input) at the boundary.
 */
public record PaymentRequest(
        @NotBlank String cartId,

        @Pattern(regexp = "\\d{16}", message = "must be 16 digits")
        String cardNumber,

        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "must be MM/YY")
        String expiry,

        @Pattern(regexp = "\\d{3}", message = "must be 3 digits")
        String cvv,

        @NotBlank String cardholderName
) {
    public String last4() {
        return cardNumber == null || cardNumber.length() < 4
                ? "????"
                : cardNumber.substring(cardNumber.length() - 4);
    }
}
