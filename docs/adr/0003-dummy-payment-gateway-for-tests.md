# ADR-0003: Use a deterministic dummy gateway for local + tests

- **Date**: 2026-05-18
- **Status**: Accepted
- **Deciders**: Checkout team

## Context

Tests and local development need a payment gateway. Options:

1. Hit the real provider's sandbox (Stripe/Razorpay test mode).
2. Mock the gateway in every test.
3. Provide a deterministic in-process fake implementation.

Option 1 is slow, requires network, and is non-deterministic during
incidents on the provider side. Option 2 makes integration tests
ceremonious and easy to forget. Option 3 lets the controller-layer tests
exercise the real Spring wiring with predictable card-number → outcome
mapping.

## Decision

We ship a `DummyPaymentGateway @Component` whose behaviour is driven by a
small table of test card numbers (see `02-Technical-Design §5` and
`CLAUDE.md §Test cards`). It satisfies the same `PaymentGateway`
interface that the real `StripeGateway` (future) will satisfy. In
non-prod profiles the dummy is wired by default; in prod the real
provider is wired via Spring profiles.

## Consequences

- **Positive**: Integration tests are deterministic, hermetic, and fast (<3 s).
- **Positive**: Onboarding works fully offline.
- **Negative**: Need to remember to *not* ship `DummyPaymentGateway` to production. Guarded by Spring `@Profile("!prod")`.
- **Neutral**: Adds <100 LOC to maintain.

## Alternatives considered

- **WireMock against Stripe API**: heavier; we'd still need our own contract assertions.
- **Hexagonal architecture with a no-op gateway**: same idea, but the *deterministic* version is more useful for tests than a pure no-op.
