# Contributing to Checkout

> Read this **once** before your first PR. After that, the PR template is your checklist.

## TL;DR

1. Pick a small slice of work.
2. Walk the 5 AIDLC stages: `01-PRD` → `02-Technical-Design` → `03-Code` → `04-Tests` → `05-Metrics`. Update only the artifacts your change touches.
3. `mvn verify` must be green.
4. Open a PR using the template — every checkbox should be either ticked or struck through with reasoning.

## Branch & commit conventions

- Branch: `feat/<short>`, `fix/<short>`, `chore/<short>`, `docs/<short>`.
- Commits: imperative present tense ("Add idempotency hit metric", not "Added").
- One logical change per PR. If the PR description has the word "and", split it.

## Stage-by-stage checklist

### Stage 1 — PRD (`01-PRD/`)
Update only if user-visible behaviour changes. New ACs **must** be in Given/When/Then format.

### Stage 2 — Technical Design (`02-Technical-Design/`)
Update if any of these change: architecture, data model, API contract, cross-cutting concerns. Add a new ADR under `docs/adr/` for any non-obvious decision.

### Stage 3 — Code (`03-Code/`)
- Follow the invariants in [`CLAUDE.md`](CLAUDE.md).
- New endpoint → also update `docs/api/openapi.yaml`.
- Money in `BigDecimal`. Card data never logged. Idempotency in DB.

### Stage 4 — Tests
- Every new AC needs a test method.
- Unit tests under `03-Code/src/test/.../service/`, integration under `.../controller/`.
- Test method name = the sentence it asserts (`returns_402_when_card_is_declined`).

### Stage 5 — Metrics
- New outcome? → new `Counter` tag in `CheckoutMetrics`.
- New SLO? → update [`docs/RUNBOOK.md §3`](docs/RUNBOOK.md#3-slos-and-alert-routing) and add an alert rule.

## Reviewing a PR

The reviewer's job is to verify the 5 boxes above are honest, not to find typos.
GitHub Actions will catch build/test/format issues automatically.

## Running things

```bash
mvn verify                  # full build + tests
mvn -Dtest=ClassName test   # single test class
mvn spring-boot:run         # local server on :8080
```

## Asking for help

`#checkout` on Slack. For prod issues see [`docs/RUNBOOK.md §7`](docs/RUNBOOK.md#7-escalation).
