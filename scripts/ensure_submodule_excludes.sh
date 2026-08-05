#!/usr/bin/env bash
# Ensure Git submodule working trees never report leaked local build-artifact
# directories (e.g. legacy Java annotation-processor outputs under `bin/`) as
# submodule changes. This is applied per submodule via Git's untracked local
# exclude file, so the parent repository never needs to commit edits inside
# the submodule itself.
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
EXCLUDE_PATTERNS=(
  'bin/'
  'out/'
)

cd "$ROOT_DIR"

if [ -d .git/modules ]; then
  find .git/modules -name exclude -path '*/info/exclude' -print0 | while IFS= read -r -d '' EXCLUDE_FILE; do
    EXISTING="$(cat "$EXCLUDE_FILE" 2>/dev/null || true)"
    MODIFIED=0
    for PATTERN in "${EXCLUDE_PATTERNS[@]}"; do
      if ! grep -Fxq "$PATTERN" <<< "$EXISTING"; then
        if [ -z "$EXISTING" ]; then
          EXISTING="$PATTERN"
        else
          EXISTING="$EXISTING"$'\n'"$PATTERN"
        fi
        MODIFIED=1
      fi
    done
    if [ "$MODIFIED" -eq 1 ]; then
      printf '%s\n' "$EXISTING" > "$EXCLUDE_FILE"
    fi
  done
fi
