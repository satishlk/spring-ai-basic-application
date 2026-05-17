package com.example.checkout.exception;

public class EmptyCartException extends CheckoutException {
    public EmptyCartException() { super("EMPTY_CART", "Cart is empty"); }
}
