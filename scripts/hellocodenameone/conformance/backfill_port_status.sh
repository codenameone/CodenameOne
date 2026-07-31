#!/usr/bin/env bash
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.

set -euo pipefail

# Publish the newest master report for every port in the compliance contract.
#
# port-status-publish.yml reacts to workflow_run events from the producing
# workflows. Those events are not delivered reliably for every producer: the
# Linux and Windows suites have never landed a single report that way, so the
# public table served a checked-in fallback for them until it aged past the
# staleness threshold and the whole column rendered as unknown. This sweep does
# not depend on an event arriving. It reads the newest completed master run of
# each producing workflow, takes the normalized report it uploaded, and
# publishes it when it is newer than the copy on the data branch.
#
# It finishes by asserting that every port has a report that is inside the
# contract's staleness window, so a producer that stops emitting reports fails
# here instead of quietly rotting on the website.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
MANIFEST="${REPO_ROOT}/docs/website/data/port_status.json"
DATA_BRANCH="port-status-data"

for tool in gh jq python3; do
  if ! command -v "${tool}" >/dev/null 2>&1; then
    echo "backfill-port-status: ${tool} is required." >&2
    exit 2
  fi
done

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

published=0
skipped=0

# One producing workflow can own several ports (the iOS suite emits four), so
# sweep per workflow and let the report itself name the port it belongs to.
while IFS= read -r workflow; do
  # Newest first, and a failed run counts: a suite that fails still uploads the
  # normalized report, and a report that records real failures is the result
  # the table is supposed to show.
  candidates="$(gh run list --workflow "${workflow}" --branch master --limit 40 \
    --json databaseId,event,conclusion,updatedAt \
    --jq '[.[] | select((.event == "push" or .event == "schedule") and
          (.conclusion == "success" or .conclusion == "failure"))]
          | sort_by(.updatedAt) | reverse | .[0:5] | .[].databaseId')"
  if [ -z "${candidates}" ]; then
    echo "No completed master run for ${workflow}; nothing to publish." >&2
    continue
  fi

  run_id=""
  download_dir="${tmp_dir}/${workflow}"
  mkdir -p "${download_dir}"
  # A run that died before the suite reported uploads no artifact at all, and
  # artifacts expire; walk back until one of the recent runs still has reports.
  for candidate in ${candidates}; do
    if gh run download "${candidate}" --pattern 'port-status-*' --dir "${download_dir}" >/dev/null 2>&1; then
      run_id="${candidate}"
      break
    fi
  done
  if [ -z "${run_id}" ]; then
    echo "No recent ${workflow} run has a port status artifact." >&2
    continue
  fi

  while IFS= read -r report; do
    port="$(jq -r '.port // empty' "${report}")"
    if [ -z "${port}" ]; then
      echo "Ignoring ${report}: it names no port." >&2
      continue
    fi
    # Publish only what the website will actually serve. A report built against
    # an older contract passes the freshness check below but is rejected by the
    # sync, which would leave the public column on its stale fallback while
    # this sweep reported success.
    if ! python3 "${SCRIPT_DIR}/port_status.py" accept --port "${port}" --report "${report}"; then
      echo "Not publishing the ${port} report from run ${run_id}: it is not usable by the website." >&2
      continue
    fi
    generated="$(jq -r '.generated_at // empty' "${report}")"
    current=""
    if gh api "repos/${GITHUB_REPOSITORY}/contents/ports/${port}.json?ref=${DATA_BRANCH}" \
        --jq '.content' 2>/dev/null | base64 --decode > "${tmp_dir}/current.json" 2>/dev/null; then
      current="$(jq -r '.generated_at // empty' "${tmp_dir}/current.json" 2>/dev/null || true)"
    fi
    if [ -n "${current}" ] && [[ ! "${generated}" > "${current}" ]]; then
      skipped=$((skipped + 1))
      continue
    fi
    echo "Publishing ${port} from run ${run_id} of ${workflow} (${generated})."
    PORT_STATUS_PUBLISH=1 "${SCRIPT_DIR}/publish_port_status.sh" "${report}"
    published=$((published + 1))
  done < <(find "${download_dir}" -type f -name 'port-status-*.json' | sort)
done < <(jq -r '[.ports[].workflow] | unique | .[]' "${MANIFEST}")

echo "Port status sweep: published ${published} report(s), ${skipped} already current."

# Assert the outcome rather than trusting it: a port whose newest published
# report is outside the staleness window renders as unknown on the public
# table, which is exactly the failure this sweep exists to prevent.
stale_days="$(jq -r '.stale_after_days' "${MANIFEST}")"
problems=()
while IFS= read -r port; do
  if ! gh api "repos/${GITHUB_REPOSITORY}/contents/ports/${port}.json?ref=${DATA_BRANCH}" \
      --jq '.content' 2>/dev/null | base64 --decode > "${tmp_dir}/check.json" 2>/dev/null; then
    problems+=("${port}: no published report")
    continue
  fi
  # Freshness alone is not enough: a published report the website rejects
  # leaves the column on its checked-in fallback, which is the state this
  # sweep exists to detect.
  if ! python3 "${SCRIPT_DIR}/port_status.py" accept --port "${port}" --report "${tmp_dir}/check.json" >/dev/null; then
    problems+=("${port}: published report is not usable by the website")
    continue
  fi
  generated="$(jq -r '.generated_at // empty' "${tmp_dir}/check.json" 2>/dev/null || true)"
  age_days="$(python3 - "${generated}" <<'PY'
import sys
from datetime import datetime, timezone

raw = sys.argv[1]
try:
    stamp = datetime.fromisoformat(raw.replace("Z", "+00:00"))
except ValueError:
    print(-1)
else:
    print(int((datetime.now(timezone.utc) - stamp).total_seconds() // 86400))
PY
)"
  if [ "${age_days}" -lt 0 ]; then
    problems+=("${port}: unreadable generated_at ${generated:-<missing>}")
  elif [ "${age_days}" -gt "${stale_days}" ]; then
    problems+=("${port}: last report is ${age_days} days old (limit ${stale_days})")
  fi
done < <(jq -r '.ports[].id' "${MANIFEST}")

if [ ${#problems[@]} -gt 0 ]; then
  echo "Ports without a current compliance report:" >&2
  printf '  %s\n' "${problems[@]}" >&2
  echo "Fix the producing workflow; the public table cannot report a port it never hears from." >&2
  exit 1
fi

echo "Every port in the contract has a report inside the ${stale_days}-day window."
