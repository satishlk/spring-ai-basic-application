package com.example.checkout.model;

import java.math.BigDecimal;

public record CartItem(String sku, String name, int qty, BigDecimal unitPrice) {
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }
}
