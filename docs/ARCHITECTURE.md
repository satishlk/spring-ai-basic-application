# Architecture

> Visual reference. For *why* a particular choice was made, see
> [`adr/`](adr/). For the original design rationale, see
> [`../02-Technical-Design/technical-design.md`](../02-Technical-Design/technical-design.md).

---

## C4 Level 1 — System context

```mermaid
flowchart LR
    Shopper((Shopper))
    subgraph "Our system"
      Checkout[Checkout Service<br/>Spring Boot]
    end
    Gateway[(Payment Gateway<br/>Stripe / Razorpay)]
    Prom[(Prometheus)]
    Logs[(Kibana / OpenSearch)]

    Shopper -- "HTTPS<br/>POST /api/checkout/pay" --> Checkout
    Checkout -- "charge()" --> Gateway
    Checkout -- "/actuator/prometheus<br/>scraped every 15s" --> Prom
    Checkout -- "JSON logs<br/>via stdout" --> Logs
```

## C4 Level 2 — Containers

```mermaid
flowchart TB
    subgraph "Checkout Service (single JVM)"
      direction TB
      C[CheckoutController]
      S[CheckoutService<br/>+ idempotency]
      G[PaymentGateway<br/>interface]
      D[DummyPaymentGateway<br/>or StripeGateway]
      OR[OrderRepository<br/>JPA]
      CR[CartRepository]
      M[CheckoutMetrics<br/>Micrometer]
    end
    DB[(Postgres / H2)]
    C --> S
    S --> G
    G -.implemented by.-> D
    S --> OR --> DB
    S --> CR
    S --> M
```

## Request flow — successful checkout

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant C as CheckoutController
    participant S as CheckoutService
    participant G as PaymentGateway
    participant R as OrderRepository
    participant M as Metrics

    B->>C: POST /api/checkout/pay (Idempotency-Key=K)
    C->>S: pay(req, K)
    S->>R: findByIdempotencyKey(K)
    R-->>S: empty
    S->>G: charge(amount, INR, card)
    G-->>S: APPROVED, txn-xyz
    S->>R: save(Order PAID, key=K)
    S->>M: recordPaid() + payLatency
    S-->>C: CheckoutResponse(PAID, orderId)
    C-->>B: 200 OK
```

## Request flow — idempotent replay

```mermaid
sequenceDiagram
    participant B as Browser (retry)
    participant S as CheckoutService
    participant R as OrderRepository
    participant G as PaymentGateway

    B->>S: pay(req, K)   %% same K as before
    S->>R: findByIdempotencyKey(K)
    R-->>S: prior Order
    Note over S,G: Gateway is NOT called again
    S-->>B: 200 OK (same orderId)
```

## Data model

```mermaid
classDiagram
    class Order {
      +String id          %% PK, "ord-" + UUID
      +String cartId
      +BigDecimal amount
      +String currency    %% "INR"
      +String transactionId
      +String last4        %% never the full PAN
      +OrderStatus status  %% PAID | DECLINED | FAILED
      +Instant createdAt
      +String idempotencyKey  %% UNIQUE INDEX
    }
    class OrderStatus {
      <<enumeration>>
      PAID
      DECLINED
      FAILED
    }
    Order --> OrderStatus
```

## Module dependency rule

```
controller ──▶ service ──▶ repository / gateway
                 │
                 └──▶ metrics
```

A lower layer never imports a higher one. CI can enforce this with
ArchUnit (planned — see [`adr/0003-archunit-layer-tests.md`](adr/0003-archunit-layer-tests.md) once written).

## Deployment topology (target)

```mermaid
flowchart LR
    subgraph "k8s namespace: checkout-prod"
      P1[checkout-pod-1]
      P2[checkout-pod-2]
      P3[checkout-pod-3]
    end
    LB[ALB / Ingress]
    PG[(Postgres RDS<br/>Multi-AZ)]
    LB --> P1
    LB --> P2
    LB --> P3
    P1 --> PG
    P2 --> PG
    P3 --> PG
```

> Why 3 pods? Idempotency is enforced at the DB layer (UNIQUE index), so
> horizontal scaling is safe. See [`adr/0002-idempotency-in-db.md`](adr/0002-idempotency-in-db.md).
