package com.example.checkout.exception;

public class CartNotFoundException extends CheckoutException {
    public CartNotFoundException(String cartId) {
        super("CART_NOT_FOUND", "Cart not found: " + cartId);
    }
}
