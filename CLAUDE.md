# Checkout Service — Project Knowledge

> **Compatible with**: Claude Code, Cursor, Aider, Google Antigravity, Copilot
> Workspace. All of them auto-read root-level project context. Place at repo
> root or symlink to the tool-specific location (`.cursorrules`,
> `CONVENTIONS.md`, `.antigravity/context.md`, `.github/copilot-instructions.md`).

---

## 📌 Session boot block (read this BEFORE doing anything in a new chat)

**Active branch**: `feat/test-cards-from-config`
**Active PR**: **#2** → https://github.com/satishlk/spring-ai-basic-application/pull/2
**Closed**: PR #1 (placeholder title, abandoned); PR #3 (sub-agent worktree leak — closed via `scripts/finalize-pr.sh`).

**Where each piece of project state lives**:

| Need to know… | Run this |
|---|---|
| Current branch | `git branch --show-current` |
| Recent commits | `git log --oneline -10` |
| Open PRs and their head SHAs | `gh pr list --state open --json number,title,headRefName,headRefOid` |
| Card count (current) | `grep -c '^      - number:' 03-Code/src/main/resources/application.yml` |
| Are tests green? | `mvn -f 03-Code/pom.xml verify` |
| Are sub-agents auto-loaded? | `ls .claude/agents/` — if `test-card-curator.md` is there, agent is available |
| Is `gh` authenticated? | `gh auth status` — if "not logged in", run `echo "$GH_TOKEN" \| gh auth login --with-token --hostname github.com` |

**Finalization recipe** (run when the PR is ready to be tidied up before merge):

```bash
bash scripts/finalize-pr.sh        # idempotent; updates PR #2 title+body, closes #3, deletes orphan branch
```

The script also persists `$GH_TOKEN` to `~/.config/gh/hosts.yml` if it's set but `gh` isn't logged in — so first-time setup auto-completes.

**What the project IS**: an end-to-end AIDLC example (5-stage flow: PRD → Design → Code → Tests → Metrics) wrapped around a Spring Boot checkout page with a dummy payment gateway. Test cards are externalized to `application.yml` and managed via a project-scoped sub-agent (`.claude/agents/test-card-curator.md`) supporting ADD / UPDATE / REMOVE.

**What the user typically asks for**:
- Adding/updating/removing test cards (→ delegate to `@agent-test-card-curator`).
- Opening / updating PRs (→ use `gh pr create`/`edit` with `--title` and `--body-file -`).
- Explaining the AIDLC flow or the project's invariants (→ point to `01-PRD/` through `05-Metrics/`).

If you (the next Claude) just want a single-paragraph orient-yourself before answering: *"This is a Java 21 / Spring Boot 3 checkout service. Test cards live in `application.yml` (currently 18 cards, last commit on `feat/test-cards-from-config`). The reusable `test-card-curator` sub-agent does ADD/UPDATE/REMOVE on cards. Work-in-progress goes to PR #2. To clean up PR #3 and finalize titles/body, run `bash scripts/finalize-pr.sh`."*

---

## 🧠 Session continuity — read this first in any new chat

A fresh Claude Code session has no memory of prior sessions. The fastest
"orient yourself" routine:

```bash
git log --oneline -10                                # what's recently happened
gh pr list --state all --limit 5                     # PR history (open + closed)
grep -c '^      - number:' 03-Code/src/main/resources/application.yml  # current card count
git status --short                                   # what's uncommitted
```

This file (`CLAUDE.md`) plus the **5 AIDLC artifacts** (`01-PRD/` through
`05-Metrics/`) are the durable context — they're checked into git and
survive any chat. **Transient state** (current branch, current PR number,
current card count) you should always re-derive from the commands above
rather than reading it from chat scrollback.

### GitHub from Claude Code — required one-time setup

`git` operations (push, pull) work out of the box because the remote is
SSH. **`gh` operations (PR create/edit, issue comment, etc.) require a
separately-stored OAuth token** — `$GH_TOKEN` in your interactive shell
is NOT inherited by Claude Code's spawned shells.

Run this **once, ever**, in your terminal:

```bash
echo "$GH_TOKEN" | gh auth login --with-token --hostname github.com
gh auth status     # → ✓ Logged in to github.com as satishlk
```

After that, every Claude Code session can `gh pr create / edit / list`
without any further setup. If `gh auth status` says "not logged in"
in a session, this step was skipped — do it now.

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
| `.claude/agents/` | **Project-scoped reusable Claude Code sub-agents** (see § Sub-agents below). |
| `.github/pull_request_template.md` | Enforces the AIDLC stage checklist on every PR. |
| `.gitignore` / `.gitattributes` | Java/Spring + SSH-key + IDE coverage. LF line endings forced. |

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

## 🧪 Test cards (DummyPaymentGateway) — CONFIG-DRIVEN

Test cards are **not in Java** any more. They live in
`03-Code/src/main/resources/application.yml` under `gateway.test-cards.cards`,
bound to the typed `com.example.checkout.config.TestCardProperties` via
`@ConfigurationProperties`. To add / update / remove a card, edit the YAML
(no Java change) — or invoke the sub-agent (next section).

**Schema** (per card):
```yaml
- number: "<digits, quoted>"      # unique key
  outcome: APPROVE | DECLINE | TIMEOUT
  reason: <snake_case>            # ONLY for DECLINE
  label: "<human note>"
```

Unknown card numbers → `gateway.test-cards.default-decline-reason`
(currently `do_not_honor`).

### 🔒 Protected cards (do NOT change outcome or remove)

These three are referenced directly in `CheckoutControllerIT`. Changing
their outcome or deleting them will break the build:

| Card | Outcome | Used by |
|---|---|---|
| `4111111111111111` | APPROVE | `payHappyPath`, `idempotentReplay…` |
| `4000000000000002` | DECLINE `insufficient_funds` | `payDeclined` |
| `4000000000000069` | TIMEOUT | `@Disabled` slow test (nightly only) |

Everything else in the catalogue is freely editable.

Full reference: `02-Technical-Design/technical-design.md §5`.

---

## 🤖 Sub-agents — `.claude/agents/`

Project-scoped reusable Claude Code sub-agents. Checked into git so every
contributor (and every AI tool that reads `.claude/agents/`) gets them
automatically.

### Current inventory

| Agent | When to use it | Tools allowed |
|-------|----------------|----------------|
| [`test-card-curator`](.claude/agents/test-card-curator.md) | Add / update / remove test cards in the dummy gateway | `Read`, `Edit`, `Bash` |

### Three invocation patterns for `test-card-curator`

```
# ADD
@agent-test-card-curator add a card 4242424242424242 that approves
@agent-test-card-curator add a card 4000…0341 that declines with expired_card

# UPDATE (modify in place by `number:` field)
@agent-test-card-curator update card 5555555555554444 — change label to "primary happy-path"
@agent-test-card-curator make card 6011000000000004 decline with reason insufficient_funds

# REMOVE
@agent-test-card-curator remove card 8000000000000010
```

The agent: locates the row, edits YAML + matching JUnit test + mirrors
into `gatewayWithDefaults()`, runs `mvn test`, posts a structured report
with `before → after` for updates. Refuses to touch the 3 protected cards.

Auto-routing also picks up natural phrasing — *"the existing 4242… card
should decline now"* will invoke the agent automatically.

### How to add a new sub-agent

See `.claude/agents/README.md` for the file format, frontmatter schema,
and the "rules of thumb" (bound the scope tightly, allowlist tools,
pin a model, tell it how to verify itself).

---

## 🐛 Common pitfalls (real lessons learned)

| Symptom | Cause | Fix |
|---|---|---|
| `BUILD FAILURE` on `mvn test` with `java.lang.UnsupportedClassVersionError` | Java 17 on `$PATH` but POM is Java 21 | `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Integration test fails with `address already in use :8080` | Local app still running | `lsof -ti:8080 \| xargs kill -9` |
| `findByIdempotencyKey` returns null when it shouldn't | Test forgot to flush — wrap arrange block in its own `@Transactional` | — |
| New endpoint not in OpenAPI | Stage 2 / `docs/api/openapi.yaml` not updated | Update before merging |
| `gh pr create` → "not logged into any GitHub hosts" even though `git push` works | **SSH key auths git, NOT the REST API.** Two different auth systems. `export GH_TOKEN=…` in your interactive shell does **not** propagate to Claude Code's fresh shells | Run **once**: `echo "$GH_TOKEN" \| gh auth login --with-token --hostname github.com` — persists to `~/.config/gh/hosts.yml`, every future shell reads it |
| GitHub blocks `git push` with "GH013: Push protection" | An SSH private key (e.g. file literally named `github` at repo root) was committed | `git filter-repo --invert-paths --path <file> --refs <branch> --force`, **rotate the key on GitHub**, force-push. The `.gitignore` already has rules for `id_rsa`, `id_ed25519`, `/github`, `*_rsa` etc. to prevent recurrence |
| New commits not appearing on the PR | The PR was **closed** (not merged) — closed PRs stop tracking branch updates | Either click "Reopen pull request" or `gh pr reopen <num>`, **or** create a fresh PR via `gh pr create` (the branch state moves with the new PR) |
| `gh` interactive wizard accepts placeholder title/body | `gh pr create` with no `--title`/`--body` opens an editor; pressing enter on defaults gives a bad PR title | Always pass `--title "…"` + `--body-file -` with a heredoc — see commit messages on this branch for templates |
| A surprise PR `Claude/…` appears against `main` you didn't open | A sub-agent ran with **`isolation: worktree`** and the harness auto-pushed its scratch branch + opened a PR for you to review | Close it: `gh pr close <num>`; delete branch: `git push origin --delete claude/<slug>`; prune local: `git worktree prune --verbose` and `rm -rf .claude/worktrees/<slug>`. **Don't merge — it's a snapshot, not the canonical work.** Canonical work lives on your feature branch (always check `gh pr list --state open` and verify head ref) |
| `git status` in the parent repo shows files modified that you didn't change | A sub-agent's worktree shares the parent `.git` so changes can bleed across | Run `git worktree list` to see all worktrees; revert unwanted edits with `git checkout HEAD -- <path>`; consider asking the agent to NOT use `isolation: worktree` in its frontmatter if this keeps happening |

---

## 📝 Definition of Done (per PR)

- [ ] PRD updated (if user-visible behaviour changes).
- [ ] Technical Design updated (if architecture, data model, or API changes).
- [ ] All Stage-4 acceptance-criterion tests still green; new ACs have new tests.
- [ ] Metric or log added (if outcome is new and observable).
- [ ] `docs/RUNBOOK.md` updated (if new URL, alert, or troubleshooting case).
- [ ] OpenAPI spec (`docs/api/openapi.yaml`) regenerated if API surface changed.
- [ ] `mvn verify` green.
- [ ] **No secrets / build artifacts staged.** Verify with `git diff --stat --cached` before committing; `.gitignore` should catch them but check anyway.
- [ ] **PR has a real title** — never accept gh's auto-generated `feat/branch-name` default. Always pass `--title "…"`.
- [ ] If adding a recurring/mechanical task, consider a `.claude/agents/<name>.md` sub-agent.

---

## 🎬 Recipes — common things to ask Claude in any future session

| Task | What to say |
|---|---|
| Add a successful card | "@agent-test-card-curator add a card NNNN that approves" |
| Add a failing card | "@agent-test-card-curator add a card NNNN that declines with reason `<snake_case>`" |
| Change an existing card | "@agent-test-card-curator update card NNNN — change reason to `<x>`" |
| Remove a card | "@agent-test-card-curator remove card NNNN" |
| Open a PR after some commits | "create a PR for the current branch with a proper title and body" — Claude will run `gh pr create --title … --body-file -` |
| Resync context | "summarize the current state of this repo" — Claude reads CLAUDE.md + git log |
| Add a new sub-agent | "create a `<name>` sub-agent that does X" — Claude follows the format in `.claude/agents/README.md` |
