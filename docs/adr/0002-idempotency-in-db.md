# ADR-0002: Enforce idempotency in the DB, not in-memory

- **Date**: 2026-05-18
- **Status**: Accepted
- **Deciders**: Checkout team

## Context

PRD FR-3 requires that a duplicate `Idempotency-Key` never triggers a
second gateway charge. The naïve implementation is an in-memory
`ConcurrentHashMap` cache inside `CheckoutService`. But:

- We will run multiple replicas (see [ARCHITECTURE.md §deployment](../ARCHITECTURE.md#deployment-topology-target)) — an in-memory map is per-pod.
- Pod restarts wipe the cache, leaving a window where a retry succeeds twice.
- Two requests with the same key hitting two different pods at the same instant would each see "no prior order" and both call the gateway.

## Decision

Idempotency is enforced by a **`UNIQUE` constraint on `orders.idempotency_key`**.
`CheckoutService.pay` first calls `orders.findByIdempotencyKey(K)`; if a
prior order exists, it is returned verbatim and no gateway call is made.
The unique index is the ultimate guard — even under race, the DB will
reject the second insert.

## Consequences

- **Positive**: Correct under horizontal scaling and pod restarts. Single source of truth (the database). No cache invalidation problem.
- **Negative**: One extra `SELECT` per request. Acceptable — indexed lookup is <1 ms.
- **Negative**: If we ever introduce sharding, we must shard on `idempotencyKey` (or keep this table unsharded).

## Alternatives considered

- **Caffeine in-process cache**: fails on horizontal scale + restarts (see above).
- **Redis with SETNX**: works but adds a hard dependency on Redis for a correctness property; the DB is already a required dependency.
- **API gateway dedup (e.g. Envoy filter)**: misplaces the responsibility — the service should own its own correctness invariants.
