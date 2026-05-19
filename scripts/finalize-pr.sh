#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# scripts/finalize-pr.sh
# -----------------------------------------------------------------------------
# One-shot, idempotent finalizer for the feat/test-cards-from-config branch.
#
# What it does (in order):
#   1. Verifies gh is authenticated; persists the token if $GH_TOKEN is set
#      and gh's config doesn't exist yet.
#   2. Updates PR #2 title and body (full body lives in scripts/pr-2-body.md
#      and lists all 18 test cards).
#   3. Closes the stale PR #3 (sub-agent worktree leak) with an explanatory
#      comment.
#   4. Deletes the orphan remote branch claude/keen-matsumoto-b4e53a.
#   5. Prints the canonical PR #2 URL.
#
# Re-runs are safe — each step is guarded so existing-good state is a no-op.
# -----------------------------------------------------------------------------
set -euo pipefail

REPO="satishlk/spring-ai-basic-application"
PR_TO_KEEP=2
PR_TO_CLOSE=3
ORPHAN_BRANCH="claude/keen-matsumoto-b4e53a"
PR_TITLE="Externalize test cards to application.yml + reusable test-card-curator sub-agent (ADD/UPDATE/REMOVE)"

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
BODY_FILE="${SCRIPT_DIR}/pr-2-body.md"

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
ok()   { printf "  \033[32m✓\033[0m %s\n" "$*"; }
warn() { printf "  \033[33m!\033[0m %s\n" "$*"; }

# ── Step 1 — ensure gh is authenticated ──────────────────────────────────────
bold "Step 1/4  Verifying gh auth…"
if gh auth status >/dev/null 2>&1; then
    ok "gh already authenticated."
elif [[ -n "${GH_TOKEN:-}" ]]; then
    warn "gh not authenticated but \$GH_TOKEN is set — persisting it now."
    echo "$GH_TOKEN" | gh auth login --with-token --hostname github.com
    ok "Token persisted to ~/.config/gh/hosts.yml — future shells will see it."
else
    echo
    echo "❌ Neither 'gh auth status' nor \$GH_TOKEN works. Cannot proceed."
    echo "   Fix: in this same terminal, run:"
    echo "     export GH_TOKEN=ghp_...      # your PAT"
    echo "     bash scripts/finalize-pr.sh  # re-run this script"
    echo "   Or run 'gh auth login' once interactively."
    exit 1
fi

# ── Step 2 — update PR #2 title + body ───────────────────────────────────────
bold "Step 2/4  Updating PR #${PR_TO_KEEP} title and body…"
if [[ ! -f "$BODY_FILE" ]]; then
    echo "❌ Missing body file: $BODY_FILE"
    exit 1
fi
gh pr edit ${PR_TO_KEEP} \
    --repo "${REPO}" \
    --title "${PR_TITLE}" \
    --body-file "${BODY_FILE}"
ok "PR #${PR_TO_KEEP} title + body updated."

# ── Step 3 — close stale PR #3 ───────────────────────────────────────────────
bold "Step 3/4  Closing stale PR #${PR_TO_CLOSE}…"
PR3_STATE=$(gh pr view ${PR_TO_CLOSE} --repo "${REPO}" --json state -q .state 2>/dev/null || echo "missing")
if [[ "$PR3_STATE" == "OPEN" ]]; then
    gh pr close ${PR_TO_CLOSE} \
        --repo "${REPO}" \
        --comment "Stale snapshot from sub-agent worktree (${ORPHAN_BRANCH}). Canonical work is in #${PR_TO_KEEP}. Closing per the worktree-leak cleanup recipe documented in CLAUDE.md §Common pitfalls."
    ok "PR #${PR_TO_CLOSE} closed."
elif [[ "$PR3_STATE" == "CLOSED" ]]; then
    ok "PR #${PR_TO_CLOSE} already closed — no action needed."
else
    warn "PR #${PR_TO_CLOSE} not found — skipping."
fi

# ── Step 4 — delete orphan remote branch ─────────────────────────────────────
bold "Step 4/4  Deleting orphan branch ${ORPHAN_BRANCH}…"
if git ls-remote --exit-code --heads origin "${ORPHAN_BRANCH}" >/dev/null 2>&1; then
    git push origin --delete "${ORPHAN_BRANCH}"
    ok "Branch deleted on origin."
else
    ok "Branch already deleted — no action needed."
fi

# Local prune as a courtesy
git remote prune origin >/dev/null 2>&1 || true

# ── Done ─────────────────────────────────────────────────────────────────────
echo
bold "✅ Finalize complete."
echo "   Canonical PR: https://github.com/${REPO}/pull/${PR_TO_KEEP}"
echo "   Title:        ${PR_TITLE}"
echo "   Catalogue:    18 test cards (see body for full table)"
