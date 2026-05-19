package com.example.checkout.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "orders",
    indexes = @Index(name = "ux_orders_idem_key", columnList = "idempotencyKey", unique = true)
)
public class Order {

    @Id
    private String id;

    private String cartId;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String last4;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    protected Order() { /* JPA */ }

    public Order(String id, String cartId, BigDecimal amount, String currency,
                 String transactionId, String last4, OrderStatus status,
                 String idempotencyKey) {
        this.id = id;
        this.cartId = cartId;
        this.amount = amount;
        this.currency = currency;
        this.transactionId = transactionId;
        this.last4 = last4;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCartId() { return cartId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getTransactionId() { return transactionId; }
    public String getLast4() { return last4; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
