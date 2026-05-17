package com.example.checkout.service;

import com.example.checkout.exception.EmptyCartException;
import com.example.checkout.exception.PaymentDeclinedException;
import com.example.checkout.metrics.CheckoutMetrics;
import com.example.checkout.model.*;
import com.example.checkout.repository.CartRepository;
import com.example.checkout.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {

    private CartRepository carts;
    private OrderRepository orders;
    private PaymentGateway gateway;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        carts   = mock(CartRepository.class);
        orders  = mock(OrderRepository.class);
        gateway = mock(PaymentGateway.class);
        service = new CheckoutService(carts, orders, gateway,
                new CheckoutMetrics(new SimpleMeterRegistry()));
    }

    private static Cart sampleCart() {
        return new Cart("c-1", List.of(
                new CartItem("BOOK-1", "Clean Code", 1, new BigDecimal("499.00")),
                new CartItem("BOOK-2", "DDIA",       1, new BigDecimal("735.00"))
        ), "INR");
    }

    private static PaymentRequest req() {
        return new PaymentRequest("c-1", "4111111111111111", "12/29", "123", "Satish K");
    }

    @Test
    void approves_and_saves_order() {                                       // AC-1
        when(carts.findById("c-1")).thenReturn(Optional.of(sampleCart()));
        when(orders.findByIdempotencyKey("K")).thenReturn(Optional.empty());
        when(gateway.charge(any(), eq("INR"), any()))
                .thenReturn(PaymentGateway.PaymentResult.approved("txn-1"));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutResponse resp = service.pay(req(), "K");

        assertThat(resp.status()).isEqualTo("PAID");
        assertThat(resp.transactionId()).isEqualTo("txn-1");
        assertThat(resp.amount()).isEqualByComparingTo("1234.00");
        verify(orders).save(any(Order.class));
    }

    @Test
    void declined_does_not_save_order() {                                   // AC-2
        when(carts.findById("c-1")).thenReturn(Optional.of(sampleCart()));
        when(orders.findByIdempotencyKey("K")).thenReturn(Optional.empty());
        when(gateway.charge(any(), any(), any()))
                .thenReturn(PaymentGateway.PaymentResult.declined("insufficient_funds"));

        assertThatThrownBy(() -> service.pay(req(), "K"))
                .isInstanceOf(PaymentDeclinedException.class)
                .hasMessageContaining("insufficient_funds");

        verify(orders, never()).save(any());
    }

    @Test
    void empty_cart_throws() {                                              // AC-3
        when(carts.findById("c-empty"))
                .thenReturn(Optional.of(new Cart("c-empty", List.of(), "INR")));
        when(orders.findByIdempotencyKey("K")).thenReturn(Optional.empty());

        var bad = new PaymentRequest("c-empty", "4111111111111111", "12/29", "123", "X");
        assertThatThrownBy(() -> service.pay(bad, "K"))
                .isInstanceOf(EmptyCartException.class);

        verify(gateway, never()).charge(any(), any(), any());
    }

    @Test
    void idempotent_replay_returns_existing() {                             // AC-4
        Order prior = new Order("ord-prior", "c-1", new BigDecimal("1234.00"), "INR",
                "txn-prior", "1111", OrderStatus.PAID, "K");
        when(orders.findByIdempotencyKey("K")).thenReturn(Optional.of(prior));

        CheckoutResponse resp = service.pay(req(), "K");

        assertThat(resp.orderId()).isEqualTo("ord-prior");
        assertThat(resp.transactionId()).isEqualTo("txn-prior");
        verifyNoInteractions(gateway);
        verify(orders, never()).save(any());
    }

    @Test
    void zero_amount_cart_is_rejected() {                                   // EC-2
        when(carts.findById("c-1")).thenReturn(Optional.of(
                new Cart("c-1", List.of(
                    new CartItem("FREE", "Free", 1, BigDecimal.ZERO)
                ), "INR")));
        when(orders.findByIdempotencyKey("K")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(req(), "K"))
                .isInstanceOf(PaymentDeclinedException.class)
                .hasMessageContaining("amount_out_of_range");
    }
}
