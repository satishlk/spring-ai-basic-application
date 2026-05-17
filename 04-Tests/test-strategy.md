# Stage 4 — Test Strategy

> **Goal of this stage:** every acceptance criterion from the PRD becomes an
> automated test. Tests are not a separate effort — they are the
> *executable form* of the PRD.

---

## 🤖 The AI prompt that produced these tests

> *Given the PRD's acceptance criteria (AC-1…AC-5) and the technical design's
> service + controller layout, generate a test plan with two layers — JUnit
> unit tests for `CheckoutService` (mocking `PaymentGateway`,
> `OrderRepository`) and Spring Boot integration tests for the controller
> using `@SpringBootTest` + `MockMvc`. Each test must be traceable to a
> specific AC.*

---

## Test pyramid for this feature

```
        ┌──────────────────────────────┐
        │  Integration  (4 tests)      │   ← real Spring context, MockMvc, real H2
        ├──────────────────────────────┤
        │  Unit — service (5 tests)    │   ← Mockito, no Spring
        ├──────────────────────────────┤
        │  Unit — gateway (3 tests)    │   ← pure Java, deterministic table
        └──────────────────────────────┘
```

We deliberately keep the unit base wide (cheap, fast, no Spring boot time)
and the integration tier narrow (slower but proves wiring).

## Traceability — PRD AC → test class & method

| AC | What it asserts | Test class & method |
|----|-----------------|---------------------|
| AC-1 Successful checkout | `4111…` → 200 + PAID + Order saved | `CheckoutControllerIT#payHappyPath` & `CheckoutServiceTest#approves_and_saves_order` |
| AC-2 Declined card | `4000…0002` → 402 + DECLINED + no order | `CheckoutControllerIT#payDeclined` & `CheckoutServiceTest#declined_does_not_save_order` |
| AC-3 Empty cart | `c-empty` → 400 EMPTY_CART | `CheckoutControllerIT#emptyCartReturns400` & `CheckoutServiceTest#empty_cart_throws` |
| AC-4 Idempotent retry | Same key → same `orderId`, no second charge | `CheckoutControllerIT#idempotentReplay` & `CheckoutServiceTest#idempotent_replay_returns_existing` |
| AC-5 Validation | bad card format → 400 VALIDATION | `CheckoutControllerIT#invalidCardFormatReturns400` |

Edge cases EC-1 (timeout) is also covered: `DummyPaymentGatewayTest#timeoutCardThrows`.

## What we mock vs. don't mock

| Component | Unit tests | Integration tests |
|-----------|-----------|-------------------|
| `PaymentGateway` | **mock** — Mockito | **real** — `DummyPaymentGateway` bean |
| `OrderRepository` | **mock** | **real** — H2 in-memory |
| `CartRepository` | **mock** | **real** — `@Repository` with seeded carts |
| HTTP layer | not exercised | `MockMvc` |

Rule of thumb: unit tests prove *logic*; integration tests prove *wiring*.

## Conventions

- Each test method's name reads as a sentence: `verb_subject_condition_expectation`.
- One logical assertion per test. Group related asserts only when they describe the same outcome.
- AAA: Arrange / Act / Assert blocks separated by blank lines.
- No `Thread.sleep` in tests except where the SUT actually depends on time (timeout test).
- Test data is local — no shared fixtures across tests.

## Running

```bash
mvn test                  # unit only
mvn verify                # unit + integration
mvn -Dtest=CheckoutServiceTest test
```
