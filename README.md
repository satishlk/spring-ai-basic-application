# AIDLC End-to-End Example — Checkout Page (Java + Spring Boot)

> **AIDLC = AI-Driven Development Life Cycle**
> A workflow where AI is a partner in every stage of building software — from
> requirement clarification → design → code → tests → operational metrics.

This repository is a *teaching example*. The feature is intentionally tiny
(a checkout page that takes a cart, calls a dummy payment gateway, and
records the order) so you can focus on the **process**, not the domain.

---

## The 5 stages of AIDLC

| Stage | Folder | Output | Who drives it | What AI does |
|-------|--------|--------|---------------|--------------|
| 1. Product Requirements (PRD) | `01-PRD/` | `product-requirements.md` | Product / business | Drafts user stories, edge cases, acceptance criteria from a one-line idea |
| 2. Technical Design | `02-Technical-Design/` | `technical-design.md` | Tech lead / architect | Produces architecture, API contracts, data model, sequence diagrams |
| 3. Code Generation | `03-Code/` | Spring Boot app | Developer | Generates classes, controllers, services, configs from the design |
| 4. Tests (Unit + Integration) | `04-Tests/` *(also `03-Code/src/test/`)* | JUnit + MockMvc tests | Developer / QA | Generates test cases from acceptance criteria + code |
| 5. Metrics & Observability | `05-Metrics/` | `metrics-and-observability.md` + Micrometer code | SRE / platform | Designs business + technical metrics, dashboards, alerts |

The **golden rule** of AIDLC: each stage produces a written artifact that
becomes the *input prompt* for the next stage. That is what makes it a
pipeline rather than ad-hoc "AI helped me code."

---

## How to walk through this example

Read in this order — each file shows both **the artifact** and **the prompt
you would give an AI to generate it**:

1. [`01-PRD/product-requirements.md`](01-PRD/product-requirements.md)
2. [`02-Technical-Design/technical-design.md`](02-Technical-Design/technical-design.md)
3. [`03-Code/`](03-Code/) — start with `pom.xml`, then walk the
   `src/main/java/com/example/checkout/` tree top-down: `model` → `repository`
   → `service` → `controller` → `metrics` → `config`.
4. [`04-Tests/test-strategy.md`](04-Tests/test-strategy.md) — and the JUnit
   sources under `03-Code/src/test/`.
5. [`05-Metrics/metrics-and-observability.md`](05-Metrics/metrics-and-observability.md)

---

## Running the app (after stage 3)

```bash
cd 03-Code
mvn spring-boot:run
# open http://localhost:8080/checkout
# metrics:  http://localhost:8080/actuator/prometheus
```

Run tests:

```bash
cd 03-Code
mvn test                 # unit tests only
mvn verify               # unit + integration
```

---

## Why follow AIDLC instead of just "vibe coding"?

| Without AIDLC | With AIDLC |
|---|---|
| AI hallucinates requirements | PRD pins down acceptance criteria first |
| Code drifts from intent | Each stage cites the previous artifact |
| Tests are an afterthought | Test cases derive from PRD acceptance criteria |
| No way to know if the feature works in prod | Metrics are designed alongside code, not bolted on |
| Hard to onboard a new engineer | The 5 documents *are* the onboarding doc |
