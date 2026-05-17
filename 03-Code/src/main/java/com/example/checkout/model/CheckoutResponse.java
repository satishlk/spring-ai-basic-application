package com.example.checkout.model;

import java.math.BigDecimal;

public record CheckoutResponse(
        String status,           // PAID | DECLINED
        String orderId,
        String transactionId,
        BigDecimal amount,
        String last4,
        String reason            // populated only on DECLINED
) {
    public static CheckoutResponse paid(Order o) {
        return new CheckoutResponse("PAID", o.getId(), o.getTransactionId(),
                o.getAmount(), o.getLast4(), null);
    }

    public static CheckoutResponse declined(String reason) {
        return new CheckoutResponse("DECLINED", null, null, null, null, reason);
    }
}
