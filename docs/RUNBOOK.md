# Runbook — Checkout Service

> **Purpose**: a 3-am-readable reference. Anyone on-call should be able to
> find the right URL, dashboard, or fix in under 60 seconds.

---

## 1. Environment URLs

| Env | App | Health | Metrics scrape | OpenAPI |
|-----|-----|--------|----------------|---------|
| **local** | http://localhost:8080 | http://localhost:8080/actuator/health | http://localhost:8080/actuator/prometheus | http://localhost:8080/v3/api-docs |
| **dev**     | https://checkout-dev.internal.example.com     | …/actuator/health | scraped by `prom-dev` | …/v3/api-docs |
| **staging** | https://checkout-staging.internal.example.com | …/actuator/health | scraped by `prom-staging` | …/v3/api-docs |
| **prod**    | https://checkout.example.com                  | …/actuator/health | scraped by `prom-prod`    | …/v3/api-docs |

> Replace `*.example.com` with your real hostnames. Keep this table in sync
> with Terraform / Helm values — it's the single source of truth for
> "where does this thing live?"

## 2. Monitoring & dashboards

| Dashboard | URL | What it shows |
|---|---|---|
| **Checkout Overview** (Grafana) | `https://grafana.example.com/d/checkout-overview` | RPS, error %, p95 latency, conversion rate gauge |
| **Checkout Outcomes** (Grafana) | `https://grafana.example.com/d/checkout-outcomes` | Stacked area: paid / declined / failed / timeout |
| **JVM & DB** (Grafana) | `https://grafana.example.com/d/checkout-jvm` | Heap, GC, Hikari pool saturation |
| **Logs — error stream** (Kibana) | `https://kibana.example.com/app/discover#?app=checkout,level=ERROR` | Live error stream |
| **Prometheus** | `https://prometheus.example.com/graph?g0.expr=checkout_outcome_total` | Raw queries |

The PromQL queries that power these are documented in
[`../05-Metrics/metrics-and-observability.md`](../05-Metrics/metrics-and-observability.md) §2.

## 3. SLOs and alert routing

| SLO | Target | Alert channel | On-call rotation |
|-----|--------|---------------|------------------|
| Availability | 99.9 % monthly | PagerDuty `checkout-page` | Checkout team |
| p95 latency `/api/checkout/pay` | ≤ 2 s | Slack `#checkout-alerts` (warn) → PD (10 min) | Checkout team |
| Decline rate | ≤ 25 % attempts | Slack `#checkout-alerts` (warn) → PD if > 40 % | Checkout + Payments |
| Gateway error rate | ≤ 1 % | PD if > 5 % for 5 min | Payments team |

## 4. Common alert playbooks

### 🔴 `Checkout5xxBurstHigh` (5xx burn rate > 14×)

1. Open the Checkout Overview dashboard.
2. Drill on `http_server_requests_seconds_count{status=~"5.."}` by `uri`.
3. Tail logs: `kubectl logs -n checkout-prod -l app=checkout --tail=200 -f`.
4. Check recent deploys: `kubectl rollout history deployment/checkout -n checkout-prod`.
5. Rollback: `kubectl rollout undo deployment/checkout -n checkout-prod`.

### 🔴 `CheckoutLatencyP95High` (p95 > 2 s for 10 min)

1. Is the gateway slow? `rate(checkout_outcome_total{result="timeout"}[5m])`
2. Is DB saturated? `hikaricp_connections_active / hikaricp_connections_max`
3. If DB → check Postgres `pg_stat_activity`; if gateway → page Payments team.

### 🟡 `CheckoutDeclineRateHigh` (declines > 40 % for 5 min)

1. Group `checkout_outcome_total{result="declined"}` by `reason` (need to add tag) and check top reason.
2. If `do_not_honor` spiked — gateway routing issue.
3. If `insufficient_funds` spiked — likely organic / fraud event, **not** a system bug. Notify Trust & Safety.

### 🟡 `CheckoutGatewayTimeoutHigh` (timeout rate > 1 %)

1. Check the gateway provider's status page (https://status.stripe.com / https://status.razorpay.com).
2. If provider is degraded — set Slack incident, no rollback needed.
3. If provider is green — open a ticket with them, attach 5 sample `transactionId` values from our logs.

## 5. Common manual interventions

| Task | Command |
|---|---|
| Tail prod logs | `kubectl logs -n checkout-prod -l app=checkout --tail=200 -f` |
| Find an order by id | Kibana search `orderId:"ord-9af3*"` |
| Trace a failed payment | Search by `idempotencyKey` MDC value across all log lines |
| Re-run a stuck deployment | `kubectl rollout restart deployment/checkout -n checkout-prod` |
| Connect to prod DB read replica | `psql $(vault read -field=url secret/checkout/prod-readonly)` |

## 6. Secrets & credentials

| Secret | Stored in | Read by |
|--------|-----------|---------|
| DB password | Vault `secret/checkout/<env>/db` | Spring via `spring.datasource.password` env |
| Gateway API key | Vault `secret/checkout/<env>/gateway` | `StripeGateway` `@Value("${gateway.api-key}")` |
| TLS cert | cert-manager (Let's Encrypt) | Ingress |

**Never commit secrets to git.** Pre-commit hook (`detect-secrets`) blocks accidental commits.

## 7. Escalation

| Situation | Escalate to |
|---|---|
| App is hard-down | Platform on-call → checkout-team-lead |
| Gateway down for >10 min | Payments team on-call |
| Data correctness suspected (double charges, missing orders) | **PAGE** checkout-team-lead immediately; freeze deploys |
| Security incident (e.g. card data in logs) | Security on-call **immediately**; do not investigate alone |

## 8. Useful links

- Source: https://github.com/example/checkout
- CI/CD: https://github.com/example/checkout/actions
- Architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- ADRs: [`adr/`](adr/)
- Onboarding: [`ONBOARDING.md`](ONBOARDING.md)
- Slack: `#checkout` (general) · `#checkout-alerts` (alerts) · `#checkout-deploys` (CI feed)
