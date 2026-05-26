#!/usr/bin/env bash
# PreToolUse hook: blocks Edit/Write that would change outcome of a protected
# test card. These three cards are referenced directly in CheckoutControllerIT;
# changing their outcome breaks CI.
#
# Protected cards:
#   4111111111111111  → APPROVE  (payHappyPath, idempotentReplay)
#   4000000000000002  → DECLINE  insufficient_funds (payDeclined)
#   4000000000000069  → TIMEOUT  (@Disabled nightly test)
#
# The hook receives the tool input on stdin as JSON. We let edits through
# unless they touch application.yml AND mention a protected card number AND
# look like they're rewriting the outcome line.

set -euo pipefail

INPUT="$(cat)"

# Cheap path: only inspect Edit/Write on application.yml
FILE_PATH="$(printf '%s' "$INPUT" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("tool_input",{}).get("file_path",""))' 2>/dev/null || echo "")"

case "$FILE_PATH" in
  */03-Code/src/main/resources/application.yml) ;;
  *) exit 0 ;;
esac

# Pull old_string + new_string (Edit) or content (Write)
PAYLOAD="$(printf '%s' "$INPUT" | python3 -c '
import json, sys
d = json.load(sys.stdin)
ti = d.get("tool_input", {})
print(ti.get("old_string","") + "\n---\n" + ti.get("new_string","") + "\n---\n" + ti.get("content",""))
' 2>/dev/null || echo "")"

PROTECTED='4111111111111111 4000000000000002 4000000000000069'

for card in $PROTECTED; do
  if printf '%s' "$PAYLOAD" | grep -q "$card"; then
    # The card is mentioned. Block only if outcome/reason appears to change.
    if printf '%s' "$PAYLOAD" | grep -Eq 'outcome:|reason:'; then
      cat >&2 <<EOF
BLOCKED: edit touches protected test card $card in application.yml.

These three cards are referenced directly in CheckoutControllerIT:
  4111111111111111  → APPROVE  (payHappyPath, idempotentReplay)
  4000000000000002  → DECLINE  insufficient_funds (payDeclined)
  4000000000000069  → TIMEOUT  (@Disabled nightly test)

Changing their outcome/reason breaks CI. If this change is intentional,
also update CheckoutControllerIT and remove this hook for the session.
EOF
      exit 2
    fi
  fi
done

exit 0
