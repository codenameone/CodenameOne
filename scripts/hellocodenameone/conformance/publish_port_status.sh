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

branch="port-status-data"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
port="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["port"])' "$report")"

# One acceptance rule, shared with the backfill sweep, rather than a second
# opinion here. This used to refuse anything whose performance run was not
# "complete" -- but a suite that crashes never reaches the benchmark, so its
# report is partial by construction and that rule dropped precisely the reports
# carrying fail / not-run evidence, leaving the table on the last green one.
# port_status.py decides; a partial performance section is simply not presented
# as performance.
accept_status=0
python3 "${script_dir}/port_status.py" accept --port "${port}" --report "${report}" || accept_status=$?
if [ "${accept_status}" -ne 0 ]; then
  case "${accept_status}" in
    11) reason="built against a different test contract; waiting for a run on the current one" ;;
    12) reason="not usable by the website" ;;
    *) reason="rejected by the publication gate (status ${accept_status})" ;;
  esac
  echo "Not publishing the ${port} report: ${reason}; preserving the last published one."
  # Contract drift is the one case that waits quietly: a report built before a
  # newly registered test is expected, and the next run resolves it. Anything
  # else -- an unusable report, or a gate failure nobody anticipated -- is a
  # producer defect, and exiting zero here made port-status-publish.yml go green
  # and rebuild the site off the PREVIOUS report, so the defect stayed invisible
  # until some later nightly sweep happened to notice it.
  if [ "${accept_status}" -eq 11 ]; then
    exit 0
  fi
  exit "${accept_status}"
fi

for tool in gh jq python3; do
  if ! command -v "${tool}" >/dev/null 2>&1; then
    echo "${tool} is required to publish port status." >&2
    exit 2
  fi
done

repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
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
generated="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8")).get("generated_at", ""))' "$report")"

# GNU spells it --decode, BSD -D.
decode_base64() {
  base64 --decode 2>/dev/null || base64 -D
}

# True when $1 is strictly later than $2; both ISO-8601 and timezone-aware, so
# they are compared as instants rather than as text.
newer_instant() {
  python3 - "$1" "$2" <<'INSTANT'
import sys
from datetime import datetime

def parse(value):
    try:
        stamp = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None
    return stamp if stamp.tzinfo is not None else None

candidate = parse(sys.argv[1])
current = parse(sys.argv[2])
sys.exit(0 if candidate is not None and (current is None or candidate > current) else 1)
INSTANT
}

for attempt in 1 2 3; do
  # Read the sha AND the stored timestamp together, and re-read both on every
  # retry. A scheduled producer's workflow_run publisher can land a newer report
  # between the sweep deciding to publish and this PUT; retrying on the fresh sha
  # alone would then overwrite it, and the sweep's own freshness check would still
  # pass because the older report it wrote is inside the window. The sha is the
  # compare-and-swap; this timestamp check is what makes the swap meaningful.
  existing_json="$(gh api "repos/${repo}/contents/${target}?ref=${branch}" 2>/dev/null || true)"
  existing_sha=""
  existing_generated=""
  if [ -n "${existing_json}" ]; then
    existing_sha="$(printf '%s' "${existing_json}" | jq -r '.sha // empty' 2>/dev/null || true)"
    existing_generated="$(printf '%s' "${existing_json}" | jq -r '.content // empty' 2>/dev/null \
      | decode_base64 2>/dev/null | jq -r '.generated_at // empty' 2>/dev/null || true)"
  fi
  if [ -n "${existing_generated}" ] && [ -n "${generated}" ] \
      && ! newer_instant "${generated}" "${existing_generated}"; then
    echo "Not publishing ${target}: the branch already holds a report at ${existing_generated} (ours is ${generated})."
    exit 0
  fi
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
