package com.example.checkout.model;

import java.math.BigDecimal;
import java.util.List;

public record Cart(String cartId, List<CartItem> items, String currency) {

    public BigDecimal grandTotal() {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
