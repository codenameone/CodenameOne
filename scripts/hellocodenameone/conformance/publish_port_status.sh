#!/usr/bin/env bash
set -euo pipefail

# Publish one normalized report to the data-only branch consumed by website CI.
# This is intentionally a per-port file update so unrelated compliance
# workflows never need to merge a shared generated document.

report="${1:-}"
if [ -z "$report" ] || [ ! -f "$report" ]; then
  echo "Usage: $0 <port-status-report.json>" >&2
  exit 2
fi

if [ "${PORT_STATUS_PUBLISH:-}" != "1" ] && { [ "${GITHUB_ACTIONS:-}" != "true" ] || [ "${GITHUB_EVENT_NAME:-}" != "push" ] || [ "${GITHUB_REF:-}" != "refs/heads/master" ]; }; then
  echo "Not a master push in GitHub Actions; skipping port-status publication."
  exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI is required to publish port status." >&2
  exit 2
fi

repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
branch="port-status-data"
port="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["port"])' "$report")"
target="ports/${port}.json"

if ! gh api "repos/${repo}/git/ref/heads/${branch}" >/dev/null 2>&1; then
  default_branch="$(gh api "repos/${repo}" --jq .default_branch)"
  base_sha="$(gh api "repos/${repo}/git/ref/heads/${default_branch}" --jq .object.sha)"
  gh api --method POST "repos/${repo}/git/refs" \
    -f ref="refs/heads/${branch}" \
    -f sha="$base_sha" >/dev/null 2>&1 || \
    gh api "repos/${repo}/git/ref/heads/${branch}" >/dev/null
fi

content="$(base64 < "$report" | tr -d '\n')"
for attempt in 1 2 3; do
  existing_sha="$(gh api "repos/${repo}/contents/${target}?ref=${branch}" --jq .sha 2>/dev/null || true)"
  args=(
    --method PUT
    "repos/${repo}/contents/${target}"
    -f message="Update ${port} compliance status"
    -f content="$content"
    -f branch="$branch"
  )
  if [ -n "$existing_sha" ]; then
    args+=(-f sha="$existing_sha")
  fi
  if gh api "${args[@]}" >/dev/null; then
    echo "Published ${target} on ${branch}."
    exit 0
  fi
  echo "Port-status update raced another publisher; retrying (${attempt}/3)." >&2
  sleep 2
done
echo "Failed to publish ${target} after three attempts." >&2
exit 1
