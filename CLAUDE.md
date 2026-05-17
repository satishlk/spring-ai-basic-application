# Checkout Service — Project Knowledge

> **Compatible with**: Claude Code, Cursor, Aider, Google Antigravity, Copilot
> Workspace. All of them auto-read root-level project context. Place at repo
> root or symlink to the tool-specific location (`.cursorrules`,
> `CONVENTIONS.md`, `.antigravity/context.md`, `.github/copilot-instructions.md`).

---

## 🚀 Quick Start (run before any change)

```bash
# Pre-reqs
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# From repo root
cd 03-Code
mvn spring-boot:run            # → http://localhost:8080/checkout

# Tests
mvn test                       # unit only (~3 s)
mvn verify                     # unit + integration (~8 s)

# Single test
mvn -Dtest=CheckoutServiceTest#approves_and_saves_order test

# Inspect live metrics
curl -s http://localhost:8080/actuator/prometheus | grep checkout_
```

If `mvn verify` is green, the PR is safe to merge.

---

## 📁 What lives where

| Folder | Purpose |
|---|---|
| `01-PRD/` | Product spec (what + why). Source of truth for acceptance criteria. |
| `02-Technical-Design/` | Architecture, API contracts, data model, design decisions. |
| `03-Code/` | The Spring Boot 3 / Java 21 app. |
| `04-Tests/` | Test strategy (the tests themselves are under `03-Code/src/test/`). |
| `05-Metrics/` | Observability plan, dashboards, SLOs, alert rules. |
| `docs/ARCHITECTURE.md` | C4-style architecture diagrams. |
| `docs/RUNBOOK.md` | URLs, dashboards, on-call, troubleshooting. |
| `docs/ONBOARDING.md` | Day-1 onboarding (read this first if you're new). |
| `docs/adr/` | Architecture Decision Records — why we chose X over Y. |
| `docs/api/openapi.yaml` | Machine-readable API contract. |

---

## 🧭 AIDLC workflow — how new features are added

**Never** start with code. Every change walks the same 5 stages and updates
the artifact in each folder before the PR opens:

`01-PRD/` → `02-Technical-Design/` → `03-Code/` → `04-Tests/` → `05-Metrics/`

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the full checklist.

---

## ⚠️ Invariants (do NOT break these)

- **Money is `BigDecimal`, never `double` or `float`.** Floating-point rounding is unacceptable for currency.
- **Card data lives in memory for the duration of a single request only.** Never logged, never persisted. Only `last4` may be stored or logged.
- **Idempotency is enforced in the DB** via `UNIQUE(idempotencyKey)` on `orders`. The in-memory check is an optimisation, not the source of truth.
- **`PaymentGateway` is the only place that talks to the outside world for money.** Add a new provider by adding a new `@Component` implementing the interface — do not edit `CheckoutService`.
- **All exceptions extend `CheckoutException`** so `GlobalExceptionHandler` can map them to the right HTTP status. Don't throw raw `RuntimeException` from services.
- **MDC keys are fixed**: `cartId`, `idempotencyKey`, `orderId`. Don't invent new ones without updating `application.yml` log pattern.
- **Every PRD acceptance criterion has a test method.** When you add an AC, add the test first; when you remove an AC, delete the test.

---

## 🎯 Stack & versions

- Java 21 (Corretto or Oracle), Spring Boot 3.3.x
- Build: Maven 3.9+
- DB: H2 in-memory for local/test; Postgres 15 in staging/prod (change `application-{env}.yml`)
- Metrics: Micrometer → Prometheus
- Logs: SLF4J + Logback, JSON in non-local profiles

---

## 🌐 URLs & environments

See [`docs/RUNBOOK.md`](docs/RUNBOOK.md) §1 for the full table.

| Env | App | Metrics | Logs |
|---|---|---|---|
| local | http://localhost:8080 | /actuator/prometheus | stdout |
| staging | https://checkout-staging.internal | https://prom-staging/.../checkout | Kibana → `app=checkout, env=staging` |
| prod | https://checkout.example.com | https://prom-prod/.../checkout | Kibana → `app=checkout, env=prod` |

> Replace the staging/prod URLs with real ones when you wire them up.

---

## 🧪 Test cards (DummyPaymentGateway)

| Card | Behaviour |
|---|---|
| `4111111111111111` | APPROVE |
| `4000000000000002` | DECLINE `insufficient_funds` |
| `4000000000000069` | sleep 6 s → `GatewayTimeoutException` (504) |
| anything else | DECLINE `do_not_honor` |

These are also documented in `02-Technical-Design/technical-design.md §5`.

---

## 🐛 Common pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| `BUILD FAILURE` on `mvn test` with `java.lang.UnsupportedClassVersionError` | Java 17 on PATH but POM is Java 21 | `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Integration test fails with `address already in use :8080` | Local app still running | `lsof -ti:8080 \| xargs kill -9` |
| `findByIdempotencyKey` returns null when it shouldn't | Test forgot to flush — wrap arrange block in its own `@Transactional` | — |
| New endpoint not in OpenAPI | Stage 2 / `docs/api/openapi.yaml` not updated | Update before merging. |

---

## 📝 Definition of Done (per PR)

- [ ] PRD updated (if user-visible behaviour changes).
- [ ] Technical Design updated (if architecture, data model, or API changes).
- [ ] All Stage-4 acceptance-criterion tests still green; new ACs have new tests.
- [ ] Metric or log added (if outcome is new and observable).
- [ ] `docs/RUNBOOK.md` updated (if new URL, alert, or troubleshooting case).
- [ ] OpenAPI spec regenerated.
- [ ] `mvn verify` green.
