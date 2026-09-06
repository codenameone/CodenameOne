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

# Authentication and malformed reports remain fatal; only the API transport
# and compare-and-swap conflicts receive bounded retries.
exec python3 "${script_dir}/publish_port_status.py" "$report" "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
