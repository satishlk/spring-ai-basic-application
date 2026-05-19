# Stage 1 — Product Requirements Document (PRD)

> **Goal of this stage:** turn a one-line business idea into a precise,
> testable set of requirements before a single line of code is written.

---

## 🤖 The AI prompt that produced this PRD

> *I want a checkout page for an online store. Customer adds items to a cart,
> reviews them, enters payment details, and we charge a (fake) payment
> gateway. Generate a complete PRD with: problem statement, user stories,
> functional + non-functional requirements, acceptance criteria in
> Given/When/Then, edge cases, and success metrics.*

You then **review the PRD line-by-line**, push back on anything wrong, and
only when it reflects reality does it become the input to Stage 2.

---

## 1. Problem statement

Buyers who reach the cart page today have no way to complete the purchase.
We need a checkout flow that (a) validates the cart, (b) collects payment
details, (c) calls our payment provider, and (d) creates an order record
the fulfilment team can act on.

## 2. Goals & non-goals

**Goals**
- Single-page checkout for one cart at a time.
- Synchronous "happy path": cart → pay → confirmation, all in one HTTP flow.
- Dummy payment gateway behind a clean interface so we can swap in Stripe/Razorpay later without changing business code.
- Idempotent: clicking "Pay" twice must not double-charge.

**Non-goals (out of scope for v1)**
- Multi-currency. v1 is INR only.
- Saved cards / wallets / UPI / EMI.
- Guest vs logged-in flows — assume logged-in.
- Tax / shipping calculation — assume already included in cart total.
- Discount codes.

## 3. User stories

| ID | As a … | I want … | So that … |
|----|--------|----------|-----------|
| US-1 | shopper | to see all my cart items, prices, and a grand total on the checkout page | I can confirm what I'm paying for |
| US-2 | shopper | to enter card details and click Pay | I can complete the purchase |
| US-3 | shopper | to see a clear success or failure message | I know whether my order went through |
| US-4 | shopper | to be told if my card is declined and retry | a transient failure does not lose my cart |
| US-5 | ops engineer | every payment attempt logged with a correlation ID | I can debug failed orders |

## 4. Functional requirements

- **FR-1 Cart retrieval.** Given a `cartId`, the system returns line items, unit prices, quantities, subtotal, and grand total. Empty cart → 400.
- **FR-2 Payment input.** The page collects: card number (16 digits), expiry (MM/YY), CVV (3 digits), cardholder name. All required, validated client-side and server-side.
- **FR-3 Idempotency.** Each Pay click must include an `Idempotency-Key` header (UUID generated on page load). Re-using the same key returns the original result, never a second charge.
- **FR-4 Charge call.** The system calls `PaymentGateway.charge(amount, currency, cardDetails)` and receives `{status, transactionId, declineReason?}`.
- **FR-5 Order creation.** On `APPROVED`, persist an `Order(id, cartId, amount, transactionId, status=PAID, createdAt)`.
- **FR-6 Failure handling.** On `DECLINED`, do **not** create an order. Surface `declineReason` to the user.
- **FR-7 Confirmation page.** On success, redirect to `/checkout/success/{orderId}` showing order id, amount, last-4 of card, transaction id.

## 5. Non-functional requirements

| Category | Requirement |
|----------|-------------|
| Performance | p95 end-to-end checkout latency ≤ 2 s under 50 RPS. |
| Availability | Checkout endpoint 99.9 % monthly. |
| Security | Card number never logged or persisted; only last-4 stored. CVV never persisted. HTTPS only in non-dev. |
| Observability | Every checkout attempt emits a structured log + metric (see §8). |
| Compliance | PCI-DSS scope minimised — card data lives only in memory during the request. |
| Accessibility | WCAG 2.1 AA on the checkout page (labels, focus order, contrast). |

## 6. Acceptance criteria (Given / When / Then)

**AC-1 Successful checkout**
- *Given* a cart with grand total ₹1,234 and a valid test card `4111-1111-1111-1111`,
- *When* the shopper submits the checkout form,
- *Then* the system returns HTTP 200 with `status=PAID` and `orderId`, and the order is persisted with `status=PAID`.

**AC-2 Declined card**
- *Given* a cart and the test card `4000-0000-0000-0002` (decline trigger),
- *When* the shopper submits,
- *Then* the system returns HTTP 402 with `status=DECLINED` and `reason="insufficient_funds"`, and **no** order is persisted.

**AC-3 Empty cart**
- *Given* an empty cart,
- *When* GET `/api/checkout/{cartId}`,
- *Then* the system returns HTTP 400 with code `EMPTY_CART`.

**AC-4 Idempotent retry**
- *Given* a successful checkout with `Idempotency-Key=K`,
- *When* the same request is submitted again with the same key,
- *Then* the system returns the original `orderId` and **no** second gateway call is made.

**AC-5 Invalid card format**
- *Given* card number `1234`,
- *When* the shopper submits,
- *Then* the system returns HTTP 400 with field-level validation errors and never calls the gateway.

## 7. Edge cases

1. Gateway times out (>5 s) → return 504, do not create order, allow retry with same idempotency key.
2. Cart total is ₹0 → reject with 400.
3. Cart total exceeds ₹10,00,000 → reject with 400 (`AMOUNT_LIMIT`).
4. User submits twice in 100 ms (double-click) — covered by FR-3 idempotency.
5. Gateway returns malformed response → treat as failure, log at ERROR.
6. Currency mismatch (cart in INR, gateway expects USD) → fail fast with 500.

## 8. Success metrics (input to Stage 5)

**Business**
- Checkout conversion rate = `paid_orders / checkout_attempts` ≥ 70 %.
- Cart abandonment after reaching checkout ≤ 30 %.

**Technical**
- p95 latency `/api/checkout/pay` ≤ 2 s.
- Gateway error rate ≤ 1 %.
- Idempotency hit rate (duplicate-prevented requests) — observed, not targeted.

## 9. Open questions

- Q1: Do we need email confirmation in v1? *(Resolved: no, separate ticket.)*
- Q2: GST invoice generation? *(Resolved: out of scope.)*
- Q3: Refund flow? *(Out of scope for v1.)*

---

## ✅ Definition of Done for Stage 1

- [x] Every user story has at least one acceptance criterion.
- [x] Every acceptance criterion is in Given / When / Then format and is testable.
- [x] Edge cases are listed.
- [x] Non-functional requirements are quantified (numbers, not adjectives).
- [x] Success metrics are defined and will become Stage 5 inputs.

The PRD is now the single source of truth for "what we are building."
Stage 2 will translate **what** into **how**.
