package com.example.checkout.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps domain exceptions onto the HTTP status codes promised in the
 * Stage 2 design (§3 — REST API spec).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyCart(EmptyCartException e) {
        return ResponseEntity.badRequest().body(body(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFound(CartNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<Map<String, Object>> handleDeclined(PaymentDeclinedException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "DECLINED");
        body.put("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    @ExceptionHandler(GatewayTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(GatewayTimeoutException e) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = new HashMap<>();
        body.put("code", "VALIDATION");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, Object>> handleCheckout(CheckoutException e) {
        return ResponseEntity.badRequest().body(body(e.getCode(), e.getMessage()));
    }

    private static Map<String, Object> body(String code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        return m;
    }
}
