# Stage 2 — Technical Design Document (TDD)

> **Goal of this stage:** translate the PRD's *what* into a concrete *how*
> — modules, contracts, data model, sequence flow, failure handling — so
> Stage 3 (code generation) has zero ambiguity.

---

## 🤖 The AI prompt that produced this design

> *Here is the PRD (paste `01-PRD/product-requirements.md`).*
> *Produce a Java Spring Boot 3 / Java 21 technical design with: high-level
> component diagram, package layout, REST API spec (request/response JSON
> schemas, status codes), data model (entities + repository), interface for
> a swappable payment gateway, sequence diagrams for happy path and decline,
> idempotency strategy, error handling, and a list of design decisions with
> rationale. Stay aligned with every FR and NFR in the PRD.*

You then **review and challenge** every design decision before it becomes
the input to Stage 3.

---

## 1. High-level architecture

```
 ┌──────────────┐     HTTPS    ┌─────────────────────────────┐
 │   Browser    │ ───────────▶ │   Spring Boot App (8080)    │
 │ checkout.html│              │                             │
 └──────────────┘              │  ┌──────────────────────┐   │
                               │  │ CheckoutController   │   │
                               │  └─────────┬────────────┘   │
                               │            ▼                │
                               │  ┌──────────────────────┐   │
                               │  │ CheckoutService      │   │
                               │  │  + IdempotencyStore  │   │
                               │  └─────┬───────────┬────┘   │
                               │        ▼           ▼        │
                               │  ┌──────────┐ ┌──────────┐  │
                               │  │ Order    │ │ Payment  │  │
                               │  │Repository│ │ Gateway  │  │
                               │  └────┬─────┘ │(interface│  │
                               │       │       └────┬─────┘  │
                               │       ▼            ▼        │
                               │   H2 / JPA   DummyPayment   │
                               │              GatewayImpl    │
                               └─────────────────────────────┘
                                            │
                                            ▼
                                   /actuator/prometheus
                                   (Micrometer metrics)
```

## 2. Package layout

```
com.example.checkout
├── CheckoutApplication.java       # Spring Boot entrypoint
├── controller
│    └── CheckoutController.java   # REST + Thymeleaf view
├── service
│    ├── CheckoutService.java      # orchestration, idempotency
│    ├── PaymentGateway.java       # interface (FR-4)
│    └── DummyPaymentGateway.java  # in-memory simulator
├── model
│    ├── Cart.java                 # value object
│    ├── CartItem.java
│    ├── PaymentRequest.java       # DTO from form
│    ├── CheckoutResponse.java     # DTO to client
│    ├── Order.java                # JPA entity
│    └── OrderStatus.java          # enum
├── repository
│    ├── OrderRepository.java
│    └── CartRepository.java       # in-memory for demo
├── exception
│    ├── CheckoutException.java
│    └── GlobalExceptionHandler.java
├── metrics
│    └── CheckoutMetrics.java      # Micrometer counters/timers
└── config
     └── WebConfig.java
```

## 3. REST API specification

### 3.1 GET `/api/checkout/{cartId}` — fetch cart for review

**200 OK**
```json
{
  "cartId": "c-1",
  "items": [
    {"sku": "BOOK-1", "name": "Clean Code", "qty": 1, "unitPrice": 499.00},
    {"sku": "BOOK-2", "name": "DDIA",       "qty": 1, "unitPrice": 735.00}
  ],
  "currency": "INR",
  "grandTotal": 1234.00
}
```

**400 Bad Request** — `{"code":"EMPTY_CART","message":"Cart is empty"}`

### 3.2 POST `/api/checkout/pay` — submit payment

**Headers:** `Idempotency-Key: <UUID>` *(required, FR-3)*

**Request body**
```json
{
  "cartId":         "c-1",
  "cardNumber":     "4111111111111111",
  "expiry":         "12/29",
  "cvv":            "123",
  "cardholderName": "Satish K"
}
```

**200 OK** — payment approved
```json
{
  "status":        "PAID",
  "orderId":       "ord-9af3…",
  "transactionId": "txn-1234",
  "amount":        1234.00,
  "last4":         "1111"
}
```

**402 Payment Required** — declined
```json
{ "status": "DECLINED", "reason": "insufficient_funds" }
```

**400 Bad Request** — validation error
```json
{ "code": "VALIDATION", "fields": { "cardNumber": "must be 16 digits" } }
```

**504 Gateway Timeout** — gateway slow (>5 s)
```json
{ "code": "GATEWAY_TIMEOUT" }
```

### 3.3 GET `/checkout` — Thymeleaf checkout page (HTML)
### 3.4 GET `/checkout/success/{orderId}` — confirmation page

## 4. Data model

```java
@Entity @Table(name = "orders")
class Order {
    @Id String id;            // "ord-" + UUID
    String cartId;
    BigDecimal amount;
    String currency;          // "INR"
    String transactionId;     // from gateway
    String last4;             // "1111"  — never the full PAN
    @Enumerated(EnumType.STRING) OrderStatus status;
    Instant createdAt;
    String idempotencyKey;    // unique index — supports FR-3
}

enum OrderStatus { PAID, DECLINED, FAILED }
```

**Indexes**
- `PK(id)`
- `UNIQUE(idempotencyKey)` ← critical: DB enforces FR-3 even under race.

## 5. Payment gateway abstraction

```java
public interface PaymentGateway {
    PaymentResult charge(BigDecimal amount, String currency, CardDetails card);
}
```

`DummyPaymentGateway` rules (lets us write deterministic tests):

| Card number | Behaviour |
|---|---|
| `4111111111111111` | APPROVE, return `txn-<rand>` |
| `4000000000000002` | DECLINE, reason `insufficient_funds` |
| `4000000000000069` | sleep 6 s → triggers timeout path |
| anything else      | DECLINE, reason `do_not_honor` |

This is the single seam where we'll swap in Stripe/Razorpay later (Open/Closed).

## 6. Sequence — happy path

```
Browser              CheckoutController     CheckoutService    PaymentGateway   OrderRepo
   │  POST /pay (key=K)      │                    │                  │             │
   ├────────────────────────▶│                    │                  │             │
   │                         │ pay(req, K)        │                  │             │
   │                         ├───────────────────▶│                  │             │
   │                         │                    │ findByKey(K)?    │             │
   │                         │                    ├─────────────────────────────▶  │ → null
   │                         │                    │ charge(amount,…) │             │
   │                         │                    ├─────────────────▶│             │
   │                         │                    │  APPROVED, txn   │             │
   │                         │                    │◀─────────────────┤             │
   │                         │                    │ save(Order PAID, K)            │
   │                         │                    ├─────────────────────────────▶  │
   │                         │  CheckoutResponse  │                  │             │
   │                         │◀───────────────────┤                  │             │
   │   200 OK { orderId }    │                    │                  │             │
   │◀────────────────────────┤                    │                  │             │
```

## 7. Sequence — idempotent retry

Same `Idempotency-Key=K` arrives again →
`OrderRepository.findByIdempotencyKey(K)` returns the prior order →
service returns it directly, **no gateway call**, no new row.

## 8. Cross-cutting concerns

| Concern | Decision |
|---|---|
| Validation | `jakarta.validation` annotations on `PaymentRequest` (`@Pattern`, `@NotBlank`). Triggered automatically by `@Valid` in controller. |
| Idempotency | `Idempotency-Key` header, UNIQUE index in DB, atomic via `OrderRepository.findByIdempotencyKey` inside `@Transactional` service method. |
| Logging | SLF4J. MDC keys: `cartId`, `idempotencyKey`, `orderId`. Card data is **never** logged — log only `last4`. |
| Error handling | `@RestControllerAdvice GlobalExceptionHandler` maps domain exceptions → status codes per §3. |
| Security | HTTPS in non-dev (Spring `server.ssl.*`). CVV never persisted — explicit static-analysis check in CI. |
| Timeouts | Gateway client uses 5 s read timeout. On timeout, throw `GatewayTimeoutException` → 504. |
| Concurrency | Stateless service. DB unique index is the source of truth for idempotency, not in-memory map. |
| Metrics | Micrometer `Timer` on `/api/checkout/pay`, `Counter` per outcome (`paid`, `declined`, `failed`, `timeout`, `idempotent_hit`). See Stage 5. |

## 9. Design decisions & rationale

| # | Decision | Why | Alternative considered |
|---|----------|-----|------------------------|
| D-1 | Single Spring Boot service, no microservices | v1 scale doesn't justify split. Faster to ship, easier to test. | Separate `payment-service` |
| D-2 | H2 in-memory DB for the demo | Zero infra to study the example. Swap to Postgres = change `application.yml` only. | Postgres from day 1 |
| D-3 | Dummy gateway as `@Component` behind interface | OCP: real provider plugs in by replacing the bean. | Hard-coded Stripe |
| D-4 | Idempotency in DB, not in app cache | Survives restarts and multi-instance deploys. Single source of truth. | Caffeine cache |
| D-5 | Thymeleaf for the checkout page | Keeps the example self-contained — no separate frontend repo. | React SPA |
| D-6 | Synchronous flow | PRD says single HTTP round-trip; complexity of async/webhooks not yet needed. | Async + webhook |
| D-7 | `BigDecimal` for money, never `double` | Floating-point rounding is unacceptable for currency. | `double` |

## 10. Traceability — PRD → design

| PRD item | Where it lives in the design |
|---|---|
| FR-1 cart retrieval | §3.1 GET `/api/checkout/{cartId}` |
| FR-2 payment input | §3.2 request body + jakarta.validation |
| FR-3 idempotency | §3.2 header + §4 unique index + §7 sequence |
| FR-4 charge call | §5 `PaymentGateway` interface |
| FR-5 order creation | §4 `Order` entity |
| FR-6 decline handling | §3.2 402 response, no order persisted |
| FR-7 confirmation page | §3.4 |
| NFR perf 2 s p95 | §8 timeouts + §5 Stage metrics |
| NFR security PCI | §8 logging policy + §4 last4-only column |

---

## ✅ Definition of Done for Stage 2

- [x] Every PRD requirement maps to a section in this doc (§10).
- [x] Public API contracts have request/response schemas + status codes.
- [x] Data model has indexes and rationale.
- [x] At least one sequence diagram per non-trivial flow.
- [x] Cross-cutting concerns (security, errors, idempotency, perf) decided.
- [x] Decisions are recorded with rationale and alternatives.

This document is the **input prompt** for Stage 3.
