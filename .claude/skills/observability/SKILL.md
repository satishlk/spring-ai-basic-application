---
name: observability
description: Look up environment URLs, metric names, MDC log keys, and dashboards for the checkout service. Use when investigating logs or metrics, when the user asks about staging/prod endpoints, what a `checkout_*` Prometheus metric means, how to query logs in Kibana, what MDC keys are emitted, or where dashboards live. Also use before adding a new metric or log field so naming stays consistent.
---

# Observability — checkout service

## Environments

| Env | App | Metrics | Logs |
|---|---|---|---|
| local | http://localhost:8080 | /actuator/prometheus | stdout |
| staging | https://checkout-staging.internal | https://prom-staging/.../checkout | Kibana → `app=checkout, env=staging` |
| prod | https://checkout.example.com | https://prom-prod/.../checkout | Kibana → `app=checkout, env=prod` |

Full table + on-call: `docs/RUNBOOK.md` §1.

## Stack

- Java 21, Spring Boot 3.3.x, Maven 3.9+
- Metrics: Micrometer → Prometheus (scrape `/actuator/prometheus`)
- Logs: SLF4J + Logback. JSON format in non-local profiles.
- DB: H2 in-memory (local/test), Postgres 15 (staging/prod) — switch via `application-{env}.yml`.

## MDC keys (fixed — do not invent new ones without updating the log pattern in `application.yml`)

- `cartId`
- `idempotencyKey`
- `orderId`

## Metric naming convention

All custom metrics are prefixed `checkout_`. To list current ones:

```bash
curl -s http://localhost:8080/actuator/prometheus | grep '^checkout_'
```

When adding a new metric: name + labels go in `05-Metrics/`, and an alert/SLO entry goes in `docs/RUNBOOK.md` if the metric is alertable.

## Definition-of-Done observability checks

- New user-visible outcome → new metric or log field.
- New alert → `docs/RUNBOOK.md` entry with the runbook query.
- Changed log shape → update Logback pattern + downstream Kibana queries.
