---
name: troubleshooting
description: Diagnose common build, test, and git failures in this Spring Boot checkout repo. Use when the user reports a `BUILD FAILURE`, `UnsupportedClassVersionError`, port 8080 conflict, push protection rejection, a surprise `Claude/...` PR, sub-agent worktree leak, or a PR that stopped accepting new commits. Also use when `gh pr create` accepts a placeholder title or the OpenAPI/PRD update was missed.
---

# Troubleshooting — checkout-aidlc-example

Match the symptom in the table; apply the fix. Each row is a real lesson learned in this repo.

| Symptom | Cause | Fix |
|---|---|---|
| `BUILD FAILURE` on `mvn test` with `java.lang.UnsupportedClassVersionError` | Java 17 on `$PATH` but POM is Java 21 | `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Integration test fails with `address already in use :8080` | Local app still running | `lsof -ti:8080 \| xargs kill -9` |
| `findByIdempotencyKey` returns null when it shouldn't | Test forgot to flush — wrap arrange block in its own `@Transactional` | — |
| New endpoint not in OpenAPI | Stage 2 / `docs/api/openapi.yaml` not updated | Update before merging |
| `gh pr create` → "not logged into any GitHub hosts" even though `git push` works | SSH key auths git, NOT the REST API. `export GH_TOKEN` in your shell does NOT propagate to Claude Code's spawned shells | See the `github-setup` skill |
| GitHub blocks `git push` with "GH013: Push protection" | An SSH private key (e.g. file literally named `github` at repo root) was committed | `git filter-repo --invert-paths --path <file> --refs <branch> --force`, **rotate the key on GitHub**, force-push |
| New commits not appearing on the PR | The PR was **closed** (not merged) — closed PRs stop tracking branch updates | `gh pr reopen <num>` or create a fresh PR |
| `gh` interactive wizard accepts placeholder title | `gh pr create` with no `--title`/`--body` opens an editor; pressing enter gives a bad PR title | Always pass `--title "…"` + `--body-file -` with a heredoc |
| Surprise PR `Claude/…` appears against `main` | A sub-agent ran with `isolation: worktree` and the harness auto-pushed its scratch branch | Close it: `gh pr close <num>`; delete branch: `git push origin --delete claude/<slug>`; prune: `git worktree prune --verbose`. **Don't merge — it's a snapshot.** |
| `git status` in parent repo shows files you didn't change | A sub-agent's worktree shares the parent `.git` | `git worktree list`; revert with `git checkout HEAD -- <path>`; remove `isolation: worktree` from the agent's frontmatter if recurring |
