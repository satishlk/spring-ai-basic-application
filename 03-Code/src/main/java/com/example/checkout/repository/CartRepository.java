package com.example.checkout.repository;

import com.example.checkout.model.Cart;
import com.example.checkout.model.CartItem;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory cart "store" for the demo. In a real system this would be a JPA
 * repository or a Redis-backed cart service. The interface stays the same so
 * Stage 3 can be replaced without touching CheckoutService.
 */
@Repository
public class CartRepository {

    private final Map<String, Cart> carts = Map.of(
            "c-1", new Cart("c-1", List.of(
                    new CartItem("BOOK-1", "Clean Code",                 1, new BigDecimal("499.00")),
                    new CartItem("BOOK-2", "Designing Data-Intensive Apps", 1, new BigDecimal("735.00"))
            ), "INR"),
            "c-empty", new Cart("c-empty", List.of(), "INR")
    );

    public Optional<Cart> findById(String cartId) {
        return Optional.ofNullable(carts.get(cartId));
    }
}
