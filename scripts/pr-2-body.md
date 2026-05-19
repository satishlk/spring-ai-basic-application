## What & why

Three related changes plus the repo hygiene baseline:

1. **Externalize test cards to config** — move the dummy-payment gateway's test-card rules from Java constants into `application.yml`, bound via a typed `@ConfigurationProperties` class (`TestCardProperties`). Adding a new test scenario no longer requires editing Java; just YAML.
2. **Add a reusable Claude Code sub-agent** at `.claude/agents/test-card-curator.md` that owns the recurring chore of "edit YAML + matching JUnit test + verify with `mvn test`". Card lifecycle becomes a one-line prompt.
3. **Three operations supported by the agent** — `ADD` (append), `UPDATE` (modify in place by `number:` field), `REMOVE` (delete). Includes a protected-card list so the agent refuses to break `CheckoutControllerIT`. Hardened with "touch only what's named" + "YAML is source of truth" + "duplicate ADD → stop and ask" rules after a real overreach incident.
4. **Repo hygiene** — `.gitignore` (Java/Spring + SSH-key filename patterns + sub-agent worktree cache), `.gitattributes` (LF line endings), AIDLC PR template.

## AIDLC stages touched

- [x] **Stage 2 — Technical Design** — `TestCardProperties` schema documented inline in `application.yml`; agent operating manual is a living design doc.
- [x] **Stage 3 — Code** — `DummyPaymentGateway` refactored: O(1) `Map<String,Card>` lookup, `switch` on `Outcome` enum (`APPROVE | DECLINE | TIMEOUT`).
- [x] **Stage 4 — Tests** — 15 new unit tests (one per non-protected card). All green.
- [x] **Stage 5 — Metrics & docs** — `.gitignore`, `.gitattributes`, `.github/pull_request_template.md`, `.claude/agents/test-card-curator.md`, `.claude/agents/README.md`, updated `CLAUDE.md` for cross-session continuity.
- [ ] ~~Stage 1 — PRD~~ — no user-visible behaviour change.

## Test card catalogue (3 → 29)

| # | Card number | Outcome | Reason / Label |
|---|---|---|---|
| 1 | `4111111111111111` | APPROVE | original — **protected** (used by `CheckoutControllerIT#payHappyPath`) |
| 2 | `4000000000000002` | DECLINE | `insufficient_funds` — **protected** (used by `CheckoutControllerIT#payDeclined`) |
| 3 | `4000000000000069` | TIMEOUT | gateway timeout simulator — **protected** (slow `@Disabled` test) |
| 4 | `5555555555554444` | APPROVE | Mastercard — primary happy-path card |
| 5 | `378282246310005`  | APPROVE | Amex (15-digit) — always approves |
| 6 | `4000000000000341` | DECLINE | `expired_card` |
| 7 | `4000000000000127` | DECLINE | `incorrect_cvc` |
| 8 | `4000000000000119` | DECLINE | `processing_error` |
| 9 | `4100000000000019` | DECLINE | `fraudulent` |
| 10 | `4242424242424242` | APPROVE | Visa — alternative happy-path |
| 11 | `4000002500003155` | DECLINE | `requires_authentication` (3-D Secure challenge) |
| 12 | `6011000000000004` | APPROVE | Discover — always approves |
| 13 | `4000000000000226` | TIMEOUT | second gateway-timeout simulator |
| 14 | `4000000000009995` | DECLINE | `stolen_card` |
| 15 | `6105105105105100` | APPROVE | Mastercard alt-BIN (`6`-prefix) |
| 16 | `8000000000000010` | DECLINE | `expired_card` — "Visa — Txn Card expired" |
| 17 | `8105105105105100` | APPROVE | Custom BIN (`8`-prefix) — always approves |
| 18 | `9105105105105100` | APPROVE | Custom BIN (`9`-prefix) — always approves |
| 19 | `6605105105105100` | APPROVE | Custom BIN (`66`-prefix) — always approves |
| 20 | `8805105105105100` | APPROVE | Custom BIN (`88`-prefix) — always approves |
| 21 | `7775105105105100` | APPROVE | Custom BIN (`77`-prefix) — always approves |
| 22 | `8888105105105100` | APPROVE | Custom BIN (`8888`-prefix) — always approves |
| 23 | `8899105105105100` | APPROVE | Custom BIN (`8899`-prefix) — always approves |
| 24 | `9999905105105100` | APPROVE | Custom BIN (`99999`-prefix) — always approves |
| 25 | `1199905105105100` | APPROVE | Custom BIN (`1199`-prefix) — always approves |
| 26 | `9999999905105100` | APPROVE | Custom BIN (`99999999`-prefix) — always approves |
| 27 | `9999999999105100` | APPROVE | Custom BIN (`9999999999`-prefix) — always approves |
| 28 | `9999999905105199` | APPROVE | Custom BIN (`9999999905`-prefix) — always approves |
| 29 | `9999999905109999` | APPROVE | Custom BIN (`99999999051`-prefix) — always approves |

Unknown card numbers fall back to `gateway.test-cards.default-decline-reason` (`do_not_honor`).

## Sub-agent — three operations, one-line prompts

```
# ADD
@agent-test-card-curator add a card 4242424242424242 that approves
@agent-test-card-curator add a card 4000000000000341 that declines with reason expired_card

# UPDATE (modify in place by `number:` field)
@agent-test-card-curator update card 5555555555554444 — change label to "primary happy-path"
@agent-test-card-curator make card 6011000000000004 decline with reason insufficient_funds

# REMOVE
@agent-test-card-curator remove card 8000000000000010
```

Each invocation: locates the row, edits YAML + matching JUnit test, mirrors into `gatewayWithDefaults()`, runs `mvn test`, posts a structured report (`before → after` for updates). Protected cards (`4111…`, `4000…0002`, `4000…0069`) refused with an explanation.

## Test evidence

```
mvn verify
[INFO] BUILD SUCCESS — Tests run: 30, Failures: 0, Errors: 0, Skipped: 2
```

The 2 skipped tests are the pre-existing `@Disabled` slow timeout cases (only run in nightly CI).

## Risk & rollback

- **Risk**: low — behaviour for the 3 protected cards is unchanged; new cards extend the table without altering existing branches.
- **Rollback**: `git revert <merge-sha>` — fully reversible. No data model or DB change.

## Notes for the reviewer

- **Branch history was rewritten** with `git filter-repo` to remove a leaked SSH private key (`github`, `github.pub`) and a 53 MB build artifact (`03-Code/target/`) accidentally committed in a prior session. **The leaked key has been rotated on GitHub.** New `.gitignore` rules block recurrence (`id_rsa`, `id_ed25519`, `/github`, `*_rsa` patterns).
- **A prior PR #1 was closed** (not merged) — that placeholder-titled wizard PR is superseded by this one.
- **A side-effect PR #3** was auto-created from a sub-agent's isolated worktree (`claude/keen-matsumoto-b4e53a`) containing reverted overreach work. Closed alongside merging this PR — see `scripts/finalize-pr.sh` for the cleanup recipe (now documented in `CLAUDE.md §Common pitfalls`).
