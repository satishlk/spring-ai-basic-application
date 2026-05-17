# Architecture Decision Records

Short, dated, immutable notes capturing **why** a non-obvious decision was
made — so future contributors don't have to reverse-engineer it.

## Format

Each ADR is one Markdown file named `NNNN-title.md`. The template is
[`template.md`](template.md).

## Index

| # | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-idempotency-in-db.md) | Enforce idempotency in the DB, not in-memory | Accepted |
| [0003](0003-dummy-payment-gateway-for-tests.md) | Use a deterministic dummy gateway for local + tests | Accepted |

## Rules

1. **Never edit an accepted ADR.** If a decision changes, write a new ADR
   that *supersedes* the old one. The old one stays, marked `Superseded by ADR-NNNN`.
2. Keep them under 1 page. If you need more, write a separate design doc and link to it.
3. Reference the ADR id (`ADR-0002`) from code comments and from `CLAUDE.md` so
   the link between code and rationale is visible.
