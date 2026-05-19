<!-- 1-line summary in the PR title. Detail here. -->

## What & why
<!-- 2-3 sentences. Link the PRD section / ticket. -->

## AIDLC stages touched
<!-- Tick every stage whose artifact changed. Strike through with ~~text~~ + a reason for stages you intentionally skip. -->

- [ ] **Stage 1 — PRD** (`01-PRD/`) — user-visible behaviour
- [ ] **Stage 2 — Technical Design** (`02-Technical-Design/`, `docs/ARCHITECTURE.md`, `docs/adr/`) — architecture / data model / API
- [ ] **Stage 3 — Code** (`03-Code/src/main/`)
- [ ] **Stage 4 — Tests** (`03-Code/src/test/`)
- [ ] **Stage 5 — Metrics & docs** (`05-Metrics/`, `docs/RUNBOOK.md`, `docs/api/openapi.yaml`)

## Invariants check
<!-- All must be true. -->
- [ ] No `double` / `float` used for money.
- [ ] No full card number / CVV in logs.
- [ ] Idempotency contract unchanged (or ADR updated).
- [ ] `GlobalExceptionHandler` covers any new exception type.

## Test evidence
```
mvn verify   # paste tail
```

## Screenshots / curl
<!-- For UI or API changes, paste a screenshot or a working curl. -->

## Risk & rollback
<!-- One sentence each. -->
- Risk:
- Rollback:
