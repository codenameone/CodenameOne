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

# Mirrors port_status.py's FUTURE_STAMP_TOLERANCE. The gate publishes a report
# stamped slightly ahead of now, so the closing assertion has to accept the same
# margin or it fails over reports this very run published.
future_skew_seconds=3600

# True when $1 is a strictly later instant than $2. Both are timezone-aware
# ISO-8601, but not necessarily normalized to Z, so they are compared as
# instants rather than as text.
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
# An unreadable candidate is never "newer"; an unreadable stored value is
# replaced, since leaving it in place would strand the port on something the
# page cannot render either.
sys.exit(0 if candidate is not None and (current is None or candidate > current) else 1)
INSTANT
}

# The contract's own freshness window bounds how far back a candidate run is
# worth considering: a report older than this is stale by definition, so there
# is nothing to be gained by looking past it.
sweep_stale_days="$(jq -r '.stale_after_days' "${MANIFEST}")"

# One producing workflow can own several ports (the iOS suite emits four), so
# sweep per workflow and let the report itself name the port it belongs to.
while IFS= read -r workflow; do
  # Newest first, and a failed run counts: a suite that fails still uploads the
  # normalized report, and a report that records real failures is the result
  # the table is supposed to show.
  #
  # workflow_dispatch counts too. The producers declare dispatch and schedule
  # rather than push, so a maintainer rerunning one on master to repair a port
  # the scheduled run missed is exactly the recovery this sweep exists to pick
  # up -- and filtering it out meant the manual fix could never reach the table.
  # Every run still inside the staleness horizon is a candidate, rather than a
  # fixed newest-five slice. A workflow whose matrix legs fail independently --
  # the Linux producer especially, whose reports are not reliably published by
  # workflow_run -- can accumulate several runs that each omit a different leg,
  # and a five-run cap then hides a perfectly good report just behind them. The
  # loop below stops as soon as every port the workflow owns is covered, so the
  # wider net costs nothing when the newest run is complete.
  horizon="$(date -u -d "${sweep_stale_days} days ago" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
    || date -u -v-"${sweep_stale_days}"d +%Y-%m-%dT%H:%M:%SZ)"
  # gh's --jq takes one expression and forwards no jq CLI options, so --arg has
  # to go to a separate jq invocation rather than being smuggled in after --jq.
  candidates="$(gh run list --workflow "${workflow}" --branch master --limit 100 \
    --json databaseId,event,conclusion,updatedAt \
    | jq -r --arg horizon "${horizon}" '[.[] | select((.event == "push" or .event == "schedule" or .event == "workflow_dispatch") and
          (.conclusion == "success" or .conclusion == "failure") and
          (.updatedAt >= $horizon))]
          | sort_by(.updatedAt) | reverse | .[].databaseId')"
  if [ -z "${candidates}" ]; then
    echo "No completed master run for ${workflow}; nothing to publish." >&2
    continue
  fi

  run_id=""
  download_dir="${tmp_dir}/${workflow}"
  mkdir -p "${download_dir}"
  # Merge across candidate runs rather than stopping at the first with any
  # artifact: a failed matrix run can upload the report for one leg only, and
  # the other ports that workflow owns would then never be considered.
  owned="$(jq -r --arg workflow "${workflow}" '.ports[] | select(.workflow == $workflow) | .id' "${MANIFEST}")"
  for candidate in ${candidates}; do
    missing=0
    for port in ${owned}; do
      if [ ! -f "${download_dir}/covered-${port}" ]; then
        missing=1
      fi
    done
    if [ "${missing}" -eq 0 ]; then
      break
    fi
    if gh run download "${candidate}" --pattern 'port-status-*' --dir "${download_dir}/run-${candidate}" >/dev/null 2>&1; then
      run_id="${candidate}"
      while IFS= read -r downloaded; do
        found="$(jq -r '.port // empty' "${downloaded}" 2>/dev/null || true)"
        if [ -z "${found}" ] || [ -f "${download_dir}/covered-${found}" ]; then
          continue
        fi
        # Only ports this workflow is declared to produce. The port is read from
        # the artifact, so a misconfigured matrix that stamped someone else's id
        # on its report would otherwise be published straight over that port's
        # entry -- Linux evidence replacing Android's genuine result, with both
        # the gate and the freshness check satisfied.
        case " ${owned} " in
          *" ${found} "*) ;;
          *)
            echo "Ignoring a report naming ${found}: ${workflow} does not produce that port." >&2
            continue
            ;;
        esac
        # Gate before marking the port covered, not after. A newest run that
        # uploaded an unusable report would otherwise claim the port and stop
        # the older candidates from being consulted, so the sweep would keep
        # serving stale data -- or fail its closing freshness assertion --
        # while a perfectly good report sat in the run behind it.
        if ! python3 "${SCRIPT_DIR}/port_status.py" accept \
            --port "${found}" --report "${downloaded}" >/dev/null 2>&1; then
          echo "Ignoring the ${found} report from run ${candidate}: not usable by the website." >&2
          continue
        fi
        cp "${downloaded}" "${download_dir}/port-status-${found}.json"
        : > "${download_dir}/covered-${found}"
      done < <(find "${download_dir}/run-${candidate}" -type f -name 'port-status-*.json' | sort)
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
    # Compare instants rather than strings. The gate accepts any timezone-aware
    # timestamp, and "2026-08-01T01:00:00+02:00" sorts after
    # "2026-08-01T00:00:00Z" while being an hour older, so a lexical test can
    # overwrite a newer report or refuse a genuinely newer one.
    if [ -n "${current}" ] && ! newer_instant "${generated}" "${current}"; then
      skipped=$((skipped + 1))
      continue
    fi
    echo "Publishing ${port} from run ${run_id} of ${workflow} (${generated})."
    PORT_STATUS_PUBLISH=1 "${SCRIPT_DIR}/publish_port_status.sh" "${report}"
    published=$((published + 1))
  done < <(find "${download_dir}" -maxdepth 1 -type f -name 'port-status-*.json' | sort)
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
    # Compare elapsed seconds, not whole days: the page marks a report stale the
  # moment its exact age passes the window, so flooring to days would keep this
  # green for almost another day after the column had already gone stale.
  age_seconds="$(python3 - "${generated}" <<'AGE'
import sys
from datetime import datetime, timezone

raw = sys.argv[1]
try:
    stamp = datetime.fromisoformat(raw.replace("Z", "+00:00"))
except ValueError:
    print("unreadable")
else:
    print("unreadable" if stamp.tzinfo is None
          else int((datetime.now(timezone.utc) - stamp).total_seconds()))
AGE
)"
  stale_seconds=$((stale_days * 86400))
  # The publication gate deliberately tolerates an hour of clock skew, so a
  # report it accepted can legitimately carry a timestamp a little ahead of
  # now. Calling that unreadable here would fail the nightly job over a report
  # the same run just published, until wall time caught up. Unparseable stays
  # -1 from the helper above and is still a problem.
  if [ "${age_seconds}" = "unreadable" ]; then
    problems+=("${port}: unreadable generated_at ${generated:-<missing>}")
  elif [ "${age_seconds}" -lt "-${future_skew_seconds}" ]; then
    problems+=("${port}: generated_at ${generated} is $(( -age_seconds / 60 )) minutes in the future")
  elif [ "${age_seconds}" -gt "${stale_seconds}" ]; then
    problems+=("${port}: last report is $((age_seconds / 3600)) hours old (limit ${stale_days} days)")
  fi
done < <(jq -r '.ports[].id' "${MANIFEST}")

if [ ${#problems[@]} -gt 0 ]; then
  echo "Ports without a current compliance report:" >&2
  printf '  %s\n' "${problems[@]}" >&2
  echo "Fix the producing workflow; the public table cannot report a port it never hears from." >&2
  exit 1
fi

echo "Every port in the contract has a report inside the ${stale_days}-day window."
