# Onboarding — Checkout Service

> Welcome. The goal: in **30 minutes** you will have the app running
> locally, all tests green, and know where to look for anything you need.

---

## Step 1 — Install prerequisites (5 min)

```bash
# Java 21 (Corretto or Oracle)
brew install --cask zulu@21    # macOS
# or: sdk install java 21.0.5-amzn

# Maven 3.9+
brew install maven

# Verify
java -version    # → 21.x
mvn -v
```

## Step 2 — Clone and run (5 min)

```bash
git clone git@github.com:example/checkout.git
cd checkout/03-Code

mvn verify                     # runs all 14 tests — should be green
mvn spring-boot:run            # → http://localhost:8080/checkout
```

Open http://localhost:8080/checkout — you should see the checkout page.
Use card `4111111111111111`, expiry `12/29`, CVV `123` — it should redirect
to a success page.

## Step 3 — Read the 5 AIDLC artifacts (15 min)

In this order — each is short on purpose:

1. [`../01-PRD/product-requirements.md`](../01-PRD/product-requirements.md) — what we built and why
2. [`../02-Technical-Design/technical-design.md`](../02-Technical-Design/technical-design.md) — how it's built
3. [`ARCHITECTURE.md`](ARCHITECTURE.md) — visual reference
4. [`../04-Tests/test-strategy.md`](../04-Tests/test-strategy.md) — how we know it works
5. [`../05-Metrics/metrics-and-observability.md`](../05-Metrics/metrics-and-observability.md) — how we know it's healthy

## Step 4 — Your first change (5 min)

A starter task to get familiar with the loop:

1. Add a new test card to `DummyPaymentGateway` — `4242424242424242` → APPROVE.
2. Add a unit test for it in `DummyPaymentGatewayTest`.
3. Run `mvn test` → green.
4. Open a PR using the template at `.github/pull_request_template.md`.

You'll touch Stage 3 (code) and Stage 4 (test) — the smallest possible AIDLC slice.

## Step 5 — Tooling tips

### IDE
- IntelliJ IDEA Community is enough. Import as a Maven project.
- Enable annotation processing (Settings → Build → Compiler → Annotation Processors).
- Recommended plugins: SonarLint, Lombok (not required), Mermaid.

### Git
- Branch naming: `feat/<short-name>`, `fix/<short-name>`, `chore/<short-name>`.
- Commits: present-tense imperative. ("Add idempotency hit metric" not "Added").
- Conventional Commits are nice-to-have, not enforced.

### AI assistant
- The repo has a [`CLAUDE.md`](../CLAUDE.md) at the root. Claude Code, Cursor,
  Aider, Antigravity, and Copilot Workspace all read it automatically.
- It contains the **invariants** (don't break these) and the quick-start
  commands. Skim it once.

## Step 6 — Who to ask

| Topic | Person / channel |
|---|---|
| Anything code-related | `#checkout` on Slack |
| Production incident | `#checkout-alerts` + page on-call (see [`RUNBOOK.md §7`](RUNBOOK.md#7-escalation)) |
| Payment provider questions | `#payments-platform` |
| Access / permissions | `#it-help` |

## Step 7 — Definition of "onboarded"

- [ ] Ran `mvn verify` successfully.
- [ ] Loaded the local checkout page and completed a fake payment.
- [ ] Read all 5 AIDLC artifacts.
- [ ] Shipped one PR (the starter task above).
- [ ] Bookmarked the runbook dashboards.

Welcome aboard 🎉
