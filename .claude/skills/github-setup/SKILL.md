---
name: github-setup
description: One-time setup to make `gh` (GitHub CLI) work from Claude Code's non-interactive shells. Use when `gh auth status` says "not logged in", when `gh pr create` / `gh pr edit` / `gh issue` fails with auth errors, or when the user wants to create a PR but git push works while gh does not. Also use when explaining why `export GH_TOKEN` in the user's interactive shell doesn't reach Claude Code.
---

# GitHub CLI setup for Claude Code

## Why this is needed

- `git push/pull/clone` use SSH (keys in `~/.ssh/`). Works out of the box.
- `gh` uses a **separate OAuth token** stored in `~/.config/gh/hosts.yml`. The REST API is HTTPS-only.
- `export GH_TOKEN=...` in an interactive shell does NOT propagate to Claude Code's spawned shells (each Bash command reads `~/.zshenv`, not `~/.zshrc`).

## The fix (run once, in the user's terminal)

```bash
echo "$GH_TOKEN" | gh auth login --with-token --hostname github.com
gh auth status     # → ✓ Logged in to github.com as <user>
```

This persists the token to `~/.config/gh/hosts.yml`. Every future Claude Code session inherits it.

## Verification

```bash
gh auth status
gh pr list --limit 1     # smoke test
```

## When creating PRs

Always pass an explicit title and body — never let `gh` auto-generate from the branch name:

```bash
gh pr create --title "real title under 70 chars" --body-file - <<'EOF'
## Summary
- bullet
- bullet

## Test plan
- [ ] mvn verify
EOF
```

## Convenience

`scripts/finalize-pr.sh` in this repo persists `$GH_TOKEN` automatically if it's set but `gh` isn't logged in — running it once also completes setup.
