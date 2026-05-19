# Project sub-agents

Markdown files in this directory define **reusable Claude Code sub-agents**
scoped to this project. They are checked into git so the whole team gets
them automatically.

> **Why this exists**: A sub-agent is a *named, self-contained_ prompt with
> its own tool allowlist. Instead of writing the same 200-word briefing
> every time you want Claude to do a recurring task, you write the
> briefing once in `<name>.md` and invoke it with `@agent-<name>`.

## Inventory

| Agent | When to use |
|-------|-------------|
| [`test-card-curator`](test-card-curator.md) | Add or modify test cards in the dummy payment gateway (`application.yml` + matching JUnit tests) and verify with `mvn test`. |

## How to invoke

Three ways:

1. **Auto-routing** — just describe the task in normal chat:
   > "Add a card `4242424242424242` that always approves."

   Claude reads each agent's `description` field and delegates if it matches.

2. **Explicit @-mention** — force a specific agent:
   > `@agent-test-card-curator add a card 4242424242424242 that approves`

3. **Whole-session pinning** — start Claude Code in agent mode:
   ```bash
   claude --agent test-card-curator
   ```

## How to write a new one

Create `<name>.md` here with this frontmatter skeleton:

```yaml
---
name: <lowercase-hyphen-name>
description: <one-paragraph "Use when …" condition Claude reads for auto-routing>
tools: Read, Edit, Bash          # comma-separated allowlist; omit to inherit all
model: sonnet                    # sonnet | opus | haiku | inherit
---

# <Agent name>
You are the <name>. Your job is …
```

Then write the body in 2nd-person imperative as if briefing a smart
colleague who just walked into the room — files to touch, files to read,
rules, verification command, output format, refusal criteria.

### Rules of thumb

- **Bound the scope tightly.** Smallest set of files, smallest set of
  tools (principle of least privilege).
- **Tell it how to verify itself.** Give the exact command + expected output.
- **Pin the model.** Reproducibility beats novelty for recurring tasks.
- **Don't paste conversation context.** A sub-agent has no memory of the
  parent chat — write the body as if a stranger were reading it cold.
- **Keep `description` action-flavoured.** It's read by Claude for
  routing; vague descriptions = vague routing.

## File precedence (for the curious)

```
Org managed > --agents CLI flag > .claude/agents/ (project)
                                 > ~/.claude/agents/ (user)
                                 > plugin-provided agents
```

So a project-level agent always wins over a user-level one with the
same name — which is what you want for project-specific behaviour.
