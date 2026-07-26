#!/usr/bin/env bash

set -euo pipefail

readonly REMOTE="${SYNDICATION_GIT_REMOTE:-origin}"
readonly BRANCH="${GITHUB_REF_NAME:-master}"
readonly MAX_PUSH_ATTEMPTS="${SYNDICATION_PUSH_ATTEMPTS:-3}"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly MERGE_SCRIPT="$SCRIPT_DIR/merge_syndication_json.py"
readonly STATE_PATHS=(
    scripts/website/syndication-state.json
    scripts/website/syndication-queue.json
)

if git diff --quiet -- "${STATE_PATHS[@]}"; then
    echo "No state or queue changes to commit."
    exit 0
fi

if ! git diff --quiet -- . \
    ':(exclude)scripts/website/syndication-state.json' \
    ':(exclude)scripts/website/syndication-queue.json' ||
    ! git diff --cached --quiet -- . \
        ':(exclude)scripts/website/syndication-state.json' \
        ':(exclude)scripts/website/syndication-queue.json'; then
    echo "Refusing to update syndication state with unrelated tracked changes." >&2
    exit 1
fi

git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'

readonly SNAPSHOT_DIR="$(mktemp -d)"
trap 'rm -rf "$SNAPSHOT_DIR"' EXIT
mkdir -p "$SNAPSHOT_DIR/base" "$SNAPSHOT_DIR/local" "$SNAPSHOT_DIR/merged"

for path in "${STATE_PATHS[@]}"; do
    mkdir -p \
        "$SNAPSHOT_DIR/base/$(dirname "$path")" \
        "$SNAPSHOT_DIR/local/$(dirname "$path")" \
        "$SNAPSHOT_DIR/merged/$(dirname "$path")"
    git show "HEAD:$path" > "$SNAPSHOT_DIR/base/$path"
    cp "$path" "$SNAPSHOT_DIR/local/$path"
    python3 -m json.tool "$SNAPSHOT_DIR/base/$path" >/dev/null
    python3 -m json.tool "$SNAPSHOT_DIR/local/$path" >/dev/null
done

# Remove the generated changes before rebasing. Reapply them with a semantic
# three-way merge so concurrent publishers can update different platform keys
# under the same post without leaving Git conflict markers in JSON.
git restore --worktree --staged --source=HEAD -- "${STATE_PATHS[@]}"

attempt=1
while true; do
    git fetch "$REMOTE" "$BRANCH"
    git rebase "$REMOTE/$BRANCH"

    for path in "${STATE_PATHS[@]}"; do
        python3 "$MERGE_SCRIPT" \
            --base "$SNAPSHOT_DIR/base/$path" \
            --local "$SNAPSHOT_DIR/local/$path" \
            --remote "$path" \
            --output "$SNAPSHOT_DIR/merged/$path"
        cp "$SNAPSHOT_DIR/merged/$path" "$path"
        python3 -m json.tool "$path" >/dev/null
    done

    git add "${STATE_PATHS[@]}"
    if git diff --staged --quiet -- "${STATE_PATHS[@]}"; then
        echo "The latest $BRANCH already contains these state changes."
        exit 0
    fi
    git commit -m "ci: record blog syndication results"

    if git push "$REMOTE" "HEAD:$BRANCH"; then
        break
    fi
    if [ "$attempt" -ge "$MAX_PUSH_ATTEMPTS" ]; then
        echo "Failed to push syndication state after $attempt attempts." >&2
        exit 1
    fi
    attempt=$((attempt + 1))
    echo "The $BRANCH branch advanced; rebuilding state commit for attempt $attempt."
    git reset --hard HEAD^
done
