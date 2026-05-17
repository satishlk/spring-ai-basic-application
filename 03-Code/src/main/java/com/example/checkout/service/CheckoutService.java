package com.example.checkout.service;

import com.example.checkout.exception.CartNotFoundException;
import com.example.checkout.exception.EmptyCartException;
import com.example.checkout.exception.GatewayTimeoutException;
import com.example.checkout.exception.PaymentDeclinedException;
import com.example.checkout.metrics.CheckoutMetrics;
import com.example.checkout.model.*;
import com.example.checkout.repository.CartRepository;
import com.example.checkout.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestrator. Owns:
 *   - cart lookup (FR-1)
 *   - idempotency check (FR-3)
 *   - gateway call (FR-4)
 *   - order persistence (FR-5)
 *   - decline / timeout outcomes (FR-6)
 * No HTTP concerns — those live in the controller.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00"); // ₹10,00,000

    private final CartRepository carts;
    private final OrderRepository orders;
    private final PaymentGateway gateway;
    private final CheckoutMetrics metrics;

    public CheckoutService(CartRepository carts,
                           OrderRepository orders,
                           PaymentGateway gateway,
                           CheckoutMetrics metrics) {
        this.carts = carts;
        this.orders = orders;
        this.gateway = gateway;
        this.metrics = metrics;
    }

    public Cart getCart(String cartId) {
        Cart cart = carts.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));
        if (cart.isEmpty()) throw new EmptyCartException();
        return cart;
    }

    @Transactional
    public CheckoutResponse pay(PaymentRequest req, String idempotencyKey) {
        long start = System.nanoTime();
        MDC.put("cartId", req.cartId());
        MDC.put("idempotencyKey", idempotencyKey);

        try {
            // FR-3: short-circuit replay.
            var existing = orders.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                metrics.recordIdempotentHit();
                log.info("Idempotent replay — returning prior order {}", existing.get().getId());
                return CheckoutResponse.paid(existing.get());
            }

            Cart cart = getCart(req.cartId());
            BigDecimal amount = cart.grandTotal();
            if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
                metrics.recordFailed();
                throw new PaymentDeclinedException("amount_out_of_range");
            }

            PaymentGateway.PaymentResult result;
            try {
                result = gateway.charge(amount, cart.currency(),
                        new PaymentGateway.CardDetails(req.cardNumber(), req.expiry(),
                                req.cvv(), req.cardholderName()));
            } catch (GatewayTimeoutException te) {
                metrics.recordTimeout();
                log.warn("Gateway timeout for cart {}", req.cartId());
                throw te;
            }

            if (result.status() == PaymentGateway.PaymentResult.Status.DECLINED) {
                metrics.recordDeclined();
                log.info("Charge declined: {}", result.declineReason());
                throw new PaymentDeclinedException(result.declineReason());
            }

            Order saved = orders.save(new Order(
                    "ord-" + UUID.randomUUID(),
                    cart.cartId(),
                    amount,
                    cart.currency(),
                    result.transactionId(),
                    req.last4(),
                    OrderStatus.PAID,
                    idempotencyKey
            ));
            metrics.recordPaid();
            MDC.put("orderId", saved.getId());
            log.info("Charge approved — order={} amount={} txn={}",
                    saved.getId(), saved.getAmount(), saved.getTransactionId());
            return CheckoutResponse.paid(saved);
        } finally {
            metrics.payLatency().record(java.time.Duration.ofNanos(System.nanoTime() - start));
            MDC.clear();
        }
    }
}
