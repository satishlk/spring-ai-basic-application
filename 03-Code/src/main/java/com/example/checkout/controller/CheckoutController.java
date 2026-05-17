package com.example.checkout.controller;

import com.example.checkout.exception.CartNotFoundException;
import com.example.checkout.model.Cart;
import com.example.checkout.model.CheckoutResponse;
import com.example.checkout.model.PaymentRequest;
import com.example.checkout.repository.OrderRepository;
import com.example.checkout.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CheckoutController {

    private final CheckoutService service;
    private final OrderRepository orders;

    public CheckoutController(CheckoutService service, OrderRepository orders) {
        this.service = service;
        this.orders = orders;
    }

    /* ---------- HTML views (Stage 2 §3.3, §3.4) ---------- */

    @GetMapping("/checkout")
    public String checkoutPage(@RequestParam(defaultValue = "c-1") String cartId, Model model) {
        Cart cart = service.getCart(cartId);
        model.addAttribute("cart", cart);
        model.addAttribute("grandTotal", cart.grandTotal());
        return "checkout";
    }

    @GetMapping("/checkout/success/{orderId}")
    public String successPage(@PathVariable String orderId, Model model) {
        var order = orders.findById(orderId)
                .orElseThrow(() -> new CartNotFoundException(orderId));
        model.addAttribute("order", order);
        return "success";
    }

    /* ---------- JSON API (Stage 2 §3.1, §3.2) ---------- */

    @GetMapping(value = "/api/checkout/{cartId}", produces = "application/json")
    @ResponseBody
    public Cart getCart(@PathVariable String cartId) {
        return service.getCart(cartId);
    }

    @PostMapping(value = "/api/checkout/pay",
                 consumes = "application/json",
                 produces = "application/json")
    @ResponseBody
    public ResponseEntity<CheckoutResponse> pay(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(service.pay(request, idempotencyKey));
    }
}
