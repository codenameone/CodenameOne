#!/usr/bin/env bash

set -euo pipefail

readonly REMOTE="${SYNDICATION_GIT_REMOTE:-origin}"
readonly BRANCH="${GITHUB_REF_NAME:-master}"
readonly MAX_PUSH_ATTEMPTS="${SYNDICATION_PUSH_ATTEMPTS:-3}"
readonly STATE_PATHS=(
    scripts/website/syndication-state.json
    scripts/website/syndication-queue.json
)

if git diff --quiet -- "${STATE_PATHS[@]}"; then
    echo "No state or queue changes to commit."
    exit 0
fi

git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'

# Scheduled workflows run against the commit that was current when GitHub
# created the run. Master can advance before this final step, so update the
# checkout while preserving the generated state and queue changes.
git fetch "$REMOTE" "$BRANCH"
git rebase --autostash "$REMOTE/$BRANCH"

git add "${STATE_PATHS[@]}"
if git diff --staged --quiet -- "${STATE_PATHS[@]}"; then
    echo "The latest $BRANCH already contains these state changes."
    exit 0
fi
git commit -m "ci: record blog syndication results"

attempt=1
while ! git push "$REMOTE" "HEAD:$BRANCH"; do
    if [ "$attempt" -ge "$MAX_PUSH_ATTEMPTS" ]; then
        echo "Failed to push syndication state after $attempt attempts." >&2
        exit 1
    fi
    attempt=$((attempt + 1))
    echo "The $BRANCH branch advanced; rebasing before push attempt $attempt."
    git fetch "$REMOTE" "$BRANCH"
    git rebase "$REMOTE/$BRANCH"
done
