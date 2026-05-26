# Checkout Service — Project Knowledge

> Java 21 / Spring Boot 3 checkout service. AIDLC-style 5-stage flow
> (`01-PRD/` → `02-Technical-Design/` → `03-Code/` → `04-Tests/` → `05-Metrics/`).
> Test cards externalised to `application.yml`, managed by the
> `test-card-curator` sub-agent.

---

## Session boot — where state lives

Re-derive transient state from commands, not from this file:

| Need to know… | Run this |
|---|---|
| Current branch | `git branch --show-current` |
| Recent commits | `git log --oneline -10` |
| Open PRs | `gh pr list --state open --json number,title,headRefName` |
| Card count | `grep -c '^      - number:' 03-Code/src/main/resources/application.yml` |
| Tests green? | `mvn -f 03-Code/pom.xml verify` |

**Active PR**: #2 → https://github.com/satishlk/spring-ai-basic-application/pull/2.
Run `bash scripts/finalize-pr.sh` to tidy titles/body before merge (idempotent).

---

## Quick Start

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
cd 03-Code
mvn spring-boot:run            # http://localhost:8080/checkout
mvn test                       # unit
mvn verify                     # unit + integration
```

If `mvn verify` is green, the PR is safe to merge.

---

## Where things live

| Folder | Purpose |
|---|---|
| `01-PRD/` → `05-Metrics/` | The five AIDLC stages |
| `03-Code/` | Spring Boot app |
| `docs/ARCHITECTURE.md`, `docs/RUNBOOK.md`, `docs/adr/`, `docs/api/openapi.yaml` | Architecture, ops, decisions, API contract |
| `.claude/agents/` | Reusable sub-agents (see below) |
| `.claude/skills/` | Task-specific skills (troubleshooting, github-setup, observability) |
| `.claude/hooks/` | Deterministic enforcement (currently: protect-cards) |

---

## ⚠️ Invariants (do NOT break these)

- **Money is `BigDecimal`** — never `double`/`float`.
- **Card data lives in memory for a single request only.** Never logged, never persisted. Only `last4` may be stored or logged.
- **Idempotency is enforced in the DB** via `UNIQUE(idempotencyKey)` on `orders`. The in-memory check is an optimisation.
- **`PaymentGateway` is the only outbound seam for money.** Add a new provider as a new `@Component` implementing the interface — do not edit `CheckoutService`.
- **All exceptions extend `CheckoutException`** so `GlobalExceptionHandler` maps them correctly. No raw `RuntimeException` from services.
- **MDC keys are fixed**: `cartId`, `idempotencyKey`, `orderId`. Don't invent new ones without updating the Logback pattern.
- **Every PRD acceptance criterion has a test method.** Add the test when adding an AC; delete it when removing an AC.

---

## 🔒 Protected test cards (CI-critical)

Referenced directly in `CheckoutControllerIT`. A `PreToolUse` hook
(`.claude/hooks/protect-cards.sh`) blocks edits to their outcome/reason.

| Card | Outcome | Used by |
|---|---|---|
| `4111111111111111` | APPROVE | `payHappyPath`, `idempotentReplay…` |
| `4000000000000002` | DECLINE `insufficient_funds` | `payDeclined` |
| `4000000000000069` | TIMEOUT | `@Disabled` slow test |

Everything else in `application.yml` is freely editable.
Schema reference: `02-Technical-Design/technical-design.md §5`.

---

## 🤖 Sub-agents

| Agent | When to use |
|---|---|
| [`test-card-curator`](.claude/agents/test-card-curator.md) | Add / update / remove test cards |

Invocation examples:

```
@agent-test-card-curator add a card 4242424242424242 that approves
@agent-test-card-curator make card 6011000000000004 decline with reason insufficient_funds
@agent-test-card-curator remove card 8000000000000010
```

Auto-routing also picks up natural phrasing.

---

## 📦 Skills (activated on demand)

- **`troubleshooting`** — build/test/git failure recipes (Java version, port 8080, push protection, surprise `Claude/...` PRs, worktree leaks).
- **`github-setup`** — one-time `gh auth login --with-token` fix when `gh` says "not logged in".
- **`observability`** — env URLs, metric names, MDC keys, dashboard locations.

The model loads these automatically when the description matches the task.

---

## 📝 Definition of Done

- [ ] PRD updated (if user-visible behaviour changes).
- [ ] Technical Design updated (if architecture/data/API changes).
- [ ] Stage-4 ACs still green; new ACs have new tests.
- [ ] Metric or log added (if outcome is new and observable).
- [ ] `docs/RUNBOOK.md` updated (if new URL, alert, or troubleshooting case).
- [ ] OpenAPI spec regenerated if API surface changed.
- [ ] `mvn verify` green.
- [ ] No secrets / build artifacts staged — `git diff --stat --cached`.
- [ ] PR has a real title — always pass `--title "…"` to `gh pr create`.
