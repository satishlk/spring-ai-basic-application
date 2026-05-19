# ADR-0001: Record architecture decisions

- **Date**: 2026-05-18
- **Status**: Accepted
- **Deciders**: Checkout team

## Context

We need a lightweight, versioned way to remember *why* design decisions
were made. Slack threads vanish, design docs sprawl, and "git blame" only
explains *what* changed.

## Decision

We adopt [Architecture Decision Records](https://adr.github.io/) (Michael
Nygard format) stored under `docs/adr/`, one Markdown file per decision,
numbered sequentially.

## Consequences

- **Positive**: New contributors can read the historical reasoning. AI assistants can cite ADRs when answering "why is this so?".
- **Negative**: Slight overhead per non-trivial decision.
- **Neutral**: Trivial day-to-day code choices don't need an ADR — use judgement.

## Alternatives considered

- **Free-form design docs in Confluence**: hard to keep in sync with the repo; not visible during code review.
- **Code comments**: too fine-grained; can't capture decisions that span multiple files.
