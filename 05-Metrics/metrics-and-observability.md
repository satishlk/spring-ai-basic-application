# Stage 5 — Application & Product Metrics

> **Goal of this stage:** measure two things — *is the system healthy?*
> (technical metrics) and *does the feature deliver business value?*
> (product metrics). Both ship with the feature, never as a follow-up.

---

## 🤖 The AI prompt that produced this

> *Given the PRD's success metrics §8 and the design's
> `CheckoutMetrics` component, propose a complete observability plan:
> the four golden signals, business KPIs, log structure, dashboards,
> and SLOs with alert thresholds. Map every metric back to a PRD line.*

---

## 1. The two layers of metrics

| Layer | Owner | Question it answers | Where it lives |
|-------|-------|---------------------|----------------|
| **Application metrics** | Engineering / SRE | "Is the system performant and reliable?" | Micrometer → Prometheus |
| **Product / business metrics** | Product / growth | "Are users completing the checkout?" | Same Prometheus + a BI roll-up (e.g. ClickHouse / Snowflake) |

Both are emitted from the **same code path** — there is no separate
"analytics pipeline". Every checkout attempt updates both layers atomically.

## 2. Catalogue of metrics

### 2.1 Application (technical)

| Metric | Type | Tags | What it tells you |
|--------|------|------|-------------------|
| `checkout_pay_latency_seconds` | Timer (histogram) | `application=checkout` | p50/p95/p99 latency of `/api/checkout/pay` |
| `checkout_outcome_total` | Counter | `result=paid\|declined\|failed\|timeout\|idempotent_hit` | Volume per outcome |
| `http_server_requests_seconds` | Timer | auto (uri, status) | Spring Boot built-in |
| `jvm_memory_used_bytes` | Gauge | auto | Container resource health |
| `hikaricp_connections_active` | Gauge | auto | DB pool saturation |

These are wired in [`CheckoutMetrics.java`](../03-Code/src/main/java/com/example/checkout/metrics/CheckoutMetrics.java) and exposed at `/actuator/prometheus`.

### 2.2 Product (business)

These are derived in PromQL — no extra code needed:

| KPI | Formula | PRD target |
|-----|---------|-----------|
| Conversion rate | `sum(rate(checkout_outcome_total{result="paid"}[5m])) / sum(rate(checkout_outcome_total{result!="idempotent_hit"}[5m]))` | ≥ 70 % |
| Decline rate | `rate(checkout_outcome_total{result="declined"}[5m]) / rate(checkout_outcome_total[5m])` | ≤ 25 % |
| Gateway error rate | `rate(checkout_outcome_total{result=~"failed\|timeout"}[5m]) / rate(checkout_outcome_total[5m])` | ≤ 1 % |
| p95 checkout latency | `histogram_quantile(0.95, rate(checkout_pay_latency_seconds_bucket[5m]))` | ≤ 2 s |

## 3. The four golden signals (Google SRE)

| Signal | Metric we use |
|--------|---------------|
| **Latency** | `checkout_pay_latency_seconds` p95 |
| **Traffic** | `rate(checkout_outcome_total[1m])` |
| **Errors** | `rate(checkout_outcome_total{result=~"failed\|timeout"}[5m])` |
| **Saturation** | `hikaricp_connections_active / hikaricp_connections_max` |

If a dashboard or alert doesn't tie back to one of these four, ask why it exists.

## 4. SLOs and alert thresholds

| SLO | Target (30-day window) | Burn-rate alert |
|-----|------------------------|-----------------|
| Availability | 99.9 % successful (non-5xx) responses | Alert if 5-min burn rate > 14× |
| Latency | 95 % of `/api/checkout/pay` ≤ 2 s | Alert if p95 > 2 s for 10 min |
| Decline rate | ≤ 25 % of attempts | Page if > 40 % for 5 min — usually a gateway issue, not user behaviour |

## 5. Logging strategy (correlated with metrics)

Structured, JSON-formatted, with these MDC keys:

```
cartId          — every line during a checkout flow
idempotencyKey  — joins multiple log lines from the same request
orderId         — present after order creation
```

The pattern is set in [`application.yml`](../03-Code/src/main/resources/application.yml) so log lines look like:

```
INFO  [c-1, K-7c8a…, ord-9af3…]  Charge approved — order=ord-9af3… amount=1234.00 txn=txn-abc
```

**Never** logged: `cardNumber`, `cvv`, full `expiry`. Only `last4` may be logged.

## 6. Dashboard layout (Grafana)

```
┌─ Row 1 — Health ─────────────────────────────┐
│  RPS   ·   Error %   ·   p95 latency        │
├─ Row 2 — Outcomes ───────────────────────────┤
│  Stacked area: paid / declined / failed     │
│  Conversion-rate gauge (target 70 %)        │
├─ Row 3 — Gateway ────────────────────────────┤
│  Timeout rate · Decline reasons (top 5)     │
├─ Row 4 — JVM / DB ───────────────────────────┤
│  Heap · GC pause · Hikari pool active       │
└──────────────────────────────────────────────┘
```

## 7. How to verify locally

```bash
cd 03-Code
mvn spring-boot:run

# Drive some traffic
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8080/api/checkout/pay \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $(uuidgen)" \
    -d '{"cartId":"c-1","cardNumber":"4111111111111111","expiry":"12/29","cvv":"123","cardholderName":"S"}' >/dev/null
done

# Inspect raw metrics
curl -s http://localhost:8080/actuator/prometheus | grep checkout_outcome_total
```

You should see lines like:
```
checkout_outcome_total{application="checkout",result="paid"} 5.0
checkout_pay_latency_seconds_count{application="checkout"} 5.0
```

## 8. Traceability — PRD §8 → metric

| PRD success metric | Implementation |
|--------------------|----------------|
| Conversion rate ≥ 70 % | §2.2 PromQL formula + dashboard gauge |
| Cart abandonment ≤ 30 % | Derived as `1 − conversion_rate` |
| p95 latency ≤ 2 s | `checkout_pay_latency_seconds` + SLO §4 |
| Gateway error rate ≤ 1 % | `result="failed\|timeout"` ratio |
| Idempotency hit rate (observed) | `result="idempotent_hit"` counter |

---

## ✅ Definition of Done for Stage 5

- [x] Every PRD success metric maps to a PromQL query.
- [x] Four golden signals are covered.
- [x] SLOs have explicit numeric targets and alert rules.
- [x] Log fields include correlation IDs and *exclude* sensitive data.
- [x] A 1-minute local verification recipe exists.

The feature is now **shippable end-to-end**: spec'd, designed, coded,
tested, and observable.
