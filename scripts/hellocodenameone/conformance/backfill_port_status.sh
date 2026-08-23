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

# port_status.py accept distinguishes "built against an older contract, wait for
# the next run" from "the report is broken". Both keep the checked-in fallback,
# but only the second is a defect worth shouting about, so the two get different
# messages rather than one blanket "not usable".
ACCEPT_CONTRACT_DRIFT=11
ACCEPT_UNUSABLE=12

# GNU coreutils spells it --decode, BSD (macOS) spells it -D. This script has a
# BSD `date` fallback already, so it is meant to run in both places.
decode_base64() {
  base64 --decode 2>/dev/null || base64 -D
}

# Explains a non-zero `accept` status without pretending drift is corruption.
describe_accept_status() {
  case "$1" in
    "${ACCEPT_CONTRACT_DRIFT}") echo "built against a different test contract; waiting for a run on the current one" ;;
    "${ACCEPT_UNUSABLE}") echo "not usable by the website" ;;
    *) echo "rejected by the publication gate (status $1)" ;;
  esac
}

# Every downloaded report in $1 that names one of the whitespace-separated port
# ids in $2. Used to tell a run that produced nothing for this workflow apart
# from the browser-evidence sidecar from one whose producer really failed.
owned_reports_in() {
  local run_dir="$1"
  local owned_ids="$2"
  local report found
  while IFS= read -r report; do
    [ -n "${report}" ] || continue
    found="$(jq -r '.port // empty' "${report}" 2>/dev/null || true)"
    [ -n "${found}" ] || continue
    case " $(printf '%s ' ${owned_ids}) " in
      *" ${found} "*) printf '%s\n' "${found}" ;;
    esac
  done < <(port_reports_in "${run_dir}")
}

# The per-port reports a run uploaded. port-status-environment.json is the
# browser-evidence sidecar rather than a port report -- it describes the
# browsers the evidence was captured in and names no port -- so scanning it as
# one recorded the newest run as having uploaded an unreadable report.
port_reports_in() {
  find "$1" -type f -name 'port-status-*.json' ! -name 'port-status-environment.json' | sort
}

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
# Ports whose newest report was rejected as malformed. Collected rather than
# fatal on the spot so the older, usable report is still published first.
unusable=()

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
  # Every terminal conclusion except cancelled/skipped counts, not just success
  # and failure. GitHub reports timed_out and startup_failure separately, and a
  # producer that times out before normalization uploads no report at all --
  # exactly the case the "newest run uploaded no port-status artifact" check
  # exists to catch. Filtering those runs out here meant they never reached it,
  # so an older still-fresh report covered the port and the sweep stayed green
  # over a producer that emitted nothing. Cancelled stays excluded: that is
  # someone superseding a run, not the producer failing.
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
  #
  # A run that is still going is not a candidate at all. This sweep is scheduled
  # while the long producers are mid-flight -- the Windows suite takes half an
  # hour, the iOS one two -- and a running job has uploaded no artifact yet, so
  # treating it as the newest candidate reported it as a producer that "uploaded
  # no port-status artifact" and failed the nightly every single night. The
  # conclusion test alone does not exclude it: `gh run list` types conclusion as
  # a string, so an in-flight run comes back as "" rather than null and sails
  # through `.conclusion != null`. Gate on status instead, which is what
  # actually distinguishes finished from running.
  horizon="$(date -u -d "${sweep_stale_days} days ago" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
    || date -u -v-"${sweep_stale_days}"d +%Y-%m-%dT%H:%M:%SZ)"
  # gh's --jq takes one expression and forwards no jq CLI options, so --arg has
  # to go to a separate jq invocation rather than being smuggled in after --jq.
  candidates="$(gh run list --workflow "${workflow}" --branch master --limit 100 \
    --json databaseId,event,status,conclusion,updatedAt \
    | jq -r --arg horizon "${horizon}" '[.[] | select((.event == "push" or .event == "schedule" or .event == "workflow_dispatch") and
          (.status == "completed") and
          (.conclusion != null and .conclusion != "" and .conclusion != "cancelled" and .conclusion != "skipped") and
          (.updatedAt >= $horizon))]
          | sort_by(.updatedAt) | reverse | .[] | "\(.databaseId):\(.event)"')"
  if [ -z "${candidates}" ]; then
    echo "No completed master run for ${workflow}; nothing to publish." >&2
    continue
  fi

  run_id=""
  # The newest SCHEDULED (or push) run gets stricter treatment than the ones
  # behind it: it must upload an artifact, and it must report every port its
  # workflow owns. A workflow_dispatch is deliberately excluded from that role.
  # Dispatches carry arbitrary inputs and routinely run a subset on purpose --
  # scripts-ios.yml with watch_only skips the GL, Metal and tv legs, and
  # scripts-javascript.yml with port_status_browser_evidence skips its screenshot
  # job entirely -- so treating whichever run happens to be newest as the
  # authoritative producer failed the sweep for ports nobody asked to run. Their
  # reports are still merged (and, being newest, still win), which is the manual
  # recovery this sweep exists to pick up; only the strictness is scoped to the
  # runs that are supposed to cover everything.
  newest_candidate=""
  # Set once the strict candidate has been processed: coverage found at or
  # before it counts as current, so a dispatch that repaired a port newer than
  # the last scheduled run is not then reported as an omission of that run.
  past_strict_candidate=0
  download_dir="${tmp_dir}/${workflow}"
  mkdir -p "${download_dir}"
  # Merge across candidate runs rather than stopping at the first with any
  # artifact: a failed matrix run can upload the report for one leg only, and
  # the other ports that workflow owns would then never be considered.
  owned="$(jq -r --arg workflow "${workflow}" '.ports[] | select(.workflow == $workflow) | .id' "${MANIFEST}")"
  for candidate_entry in ${candidates}; do
    candidate="${candidate_entry%%:*}"
    candidate_event="${candidate_entry##*:}"
    if [ -n "${newest_candidate}" ]; then
      past_strict_candidate=1
    fi
    missing=0
    for port in ${owned}; do
      if [ ! -f "${download_dir}/covered-${port}" ]; then
        missing=1
      fi
    done
    if [ "${missing}" -eq 0 ]; then
      break
    fi
    if ! gh run download "${candidate}" --pattern 'port-status-*' --dir "${download_dir}/run-${candidate}" >/dev/null 2>&1; then
      # No report artifact at all. For the newest run that is a producer failure
      # in its own right -- a job that died before normalization uploads nothing,
      # so falling back to an older artifact covers every port, the closing
      # freshness check passes, and the sweep goes green while the current run
      # produced no evidence. Only the newest is reported: older candidates
      # without artifacts are just how the merge walks back.
      if [ -z "${newest_candidate}" ] && [ "${candidate_event}" != "workflow_dispatch" ]; then
        newest_candidate="${candidate}"
        unusable+=("${workflow}: newest run ${candidate} uploaded no port-status artifact")
      fi
      continue
    fi
    if [ -z "${newest_candidate}" ] && [ "${candidate_event}" != "workflow_dispatch" ]; then
      newest_candidate="${candidate}"
    fi
    run_id="${candidate}"
    while IFS= read -r downloaded; do
      found="$(jq -r '.port // empty' "${downloaded}" 2>/dev/null || true)"
      if [ -z "${found}" ]; then
        # Invalid JSON, or an object with no "port": the artifact names nothing,
        # so it cannot be matched to a port or gated. Skipping quietly let an
        # older valid artifact cover the workflow and the sweep finish green
        # while the newest producer output was malformed.
        echo "Ignoring $(basename "${downloaded}") from run ${candidate}: it names no port." >&2
        unusable+=("${workflow}: run ${candidate} uploaded $(basename "${downloaded}") with no readable port id")
        continue
      fi
      if [ -f "${download_dir}/covered-${found}" ]; then
        continue
      fi
      # Only ports this workflow is declared to produce. The port is read from
      # the artifact, so a misconfigured matrix that stamped someone else's id
      # on its report would otherwise be published straight over that port's
      # entry -- Linux evidence replacing Android's genuine result, with both
      # the gate and the freshness check satisfied.
      # owned is newline-separated (one id per jq row); normalise to spaces
      # so the space-delimited membership test below actually matches the ids
      # in the middle of the list rather than only the first and last.
      case " $(printf '%s ' ${owned}) " in
        *" ${found} "*) ;;
        *)
          echo "Ignoring a report naming ${found}: ${workflow} does not produce that port." >&2
          # Same standing as a malformed report, and for the same reason: the
          # sweep can still find an older, correct artifact for the port this
          # workflow really owns, and while that one stays fresh the closing
          # check passes and the job goes green over a producer that is
          # emitting misidentified data. Preserve the good report, then fail.
          unusable+=("${workflow}: run ${candidate} uploaded a report naming ${found}, which it does not produce")
          continue
          ;;
      esac
      # Gate before marking the port covered, not after. A newest run that
      # uploaded an unusable report would otherwise claim the port and stop
      # the older candidates from being consulted, so the sweep would keep
      # serving stale data -- or fail its closing freshness assertion --
      # while a perfectly good report sat in the run behind it.
      accept_status=0
      python3 "${SCRIPT_DIR}/port_status.py" accept \
          --port "${found}" --report "${downloaded}" >/dev/null 2>&1 || accept_status=$?
      if [ "${accept_status}" -ne 0 ]; then
        echo "Ignoring the ${found} report from run ${candidate}: $(describe_accept_status "${accept_status}")." >&2
        if [ "${past_strict_candidate}" -eq 0 ] \
            && [ "${accept_status}" -eq "${ACCEPT_CONTRACT_DRIFT}" ]; then
          # The newest run DID report this port; its report is simply built
          # against another revision of the contract, which is the one case that
          # is meant to wait quietly for the next run. Without this marker the
          # omission check below would see no newest-covered file, call the port
          # omitted and fail the sweep -- turning the documented quiet fallback
          # into a hard failure every time the merge behind it succeeded.
          : > "${download_dir}/newest-drift-${found}"
        fi
        # A malformed report is a producer defect, and falling back to an older
        # run hides it: the fallback is still inside the freshness window, so the
        # closing assertion passes and the sweep goes green while the newest run
        # is broken. Remember it and fail at the end -- after the older report has
        # been preserved, so the table keeps showing something rather than
        # nothing. Contract drift stays quiet, because waiting for a run on the
        # current contract is the intended behaviour there, not a defect.
        if [ "${accept_status}" -eq "${ACCEPT_UNUSABLE}" ] \
            && [ ! -f "${download_dir}/covered-${found}" ]; then
          unusable+=("${found}: run ${candidate} uploaded a report the website cannot use")
        fi
        continue
      fi
      cp "${downloaded}" "${download_dir}/port-status-${found}.json"
      : > "${download_dir}/covered-${found}"
      # At or before the strict candidate: a dispatch newer than the last
      # scheduled run counts as current coverage, so a manual repair is not
      # reported as that run having omitted the port.
      if [ "${past_strict_candidate}" -eq 0 ]; then
        : > "${download_dir}/newest-covered-${found}"
      fi
      # Remember which run this port's report actually came from. Reports are
      # merged across candidates on purpose, so a single run_id would credit
      # every port to whichever candidate happened to be examined last --
      # misleading exactly when someone is chasing down a bad report.
      printf '%s' "${candidate}" > "${download_dir}/source-run-${found}"
    done < <(port_reports_in "${download_dir}/run-${candidate}")
  done
  if [ -z "${run_id}" ]; then
    echo "No recent ${workflow} run has a port status artifact." >&2
    continue
  fi

  # A multi-port producer (the iOS suite emits four, Linux two) can upload some
  # of its ports and lose the rest when one leg dies before normalization. The
  # merge then fills those from an older run and everything looks current, so
  # the newest leg's failure is masked. Only reported when the newest run
  # produced something: a newest run with no artifact at all is already recorded
  # above, and saying both would be the same defect twice.
  if [ -d "${download_dir}/run-${newest_candidate}" ]; then
    for port in ${owned}; do
      if [ -f "${download_dir}/covered-${port}" ] \
          && [ ! -f "${download_dir}/newest-covered-${port}" ] \
          && [ ! -f "${download_dir}/newest-drift-${port}" ]; then
        unusable+=("${workflow}: newest run ${newest_candidate} did not report ${port}; an older run supplied it")
      fi
    done
  fi

  while IFS= read -r report; do
    port="$(jq -r '.port // empty' "${report}")"
    if [ -z "${port}" ]; then
      echo "Ignoring ${report}: it names no port." >&2
      continue
    fi
    source_run="${run_id}"
    if [ -f "${download_dir}/source-run-${port}" ]; then
      source_run="$(cat "${download_dir}/source-run-${port}")"
    fi
    # Publish only what the website will actually serve. A report built against
    # an older contract passes the freshness check below but is rejected by the
    # sync, which would leave the public column on its stale fallback while
    # this sweep reported success.
    accept_status=0
    python3 "${SCRIPT_DIR}/port_status.py" accept --port "${port}" --report "${report}" || accept_status=$?
    if [ "${accept_status}" -ne 0 ]; then
      echo "Not publishing the ${port} report from run ${source_run}: $(describe_accept_status "${accept_status}")." >&2
      continue
    fi
    generated="$(jq -r '.generated_at // empty' "${report}")"
    current=""
    if gh api "repos/${GITHUB_REPOSITORY}/contents/ports/${port}.json?ref=${DATA_BRANCH}" \
        --jq '.content' 2>/dev/null | decode_base64 > "${tmp_dir}/current.json" 2>/dev/null; then
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
    echo "Publishing ${port} from run ${source_run} of ${workflow} (${generated})."
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
published_dir="${tmp_dir}/published"
mkdir -p "${published_dir}"
while IFS= read -r port; do
  if ! gh api "repos/${GITHUB_REPOSITORY}/contents/ports/${port}.json?ref=${DATA_BRANCH}" \
      --jq '.content' 2>/dev/null | decode_base64 > "${tmp_dir}/check.json" 2>/dev/null; then
    problems+=("${port}: no published report")
    continue
  fi
  cp "${tmp_dir}/check.json" "${published_dir}/${port}.json"
  # Freshness alone is not enough: a published report the website rejects
  # leaves the column on its checked-in fallback, which is the state this
  # sweep exists to detect.
  accept_status=0
  python3 "${SCRIPT_DIR}/port_status.py" accept --port "${port}" --report "${tmp_dir}/check.json" >/dev/null || accept_status=$?
  if [ "${accept_status}" -ne 0 ]; then
    problems+=("${port}: published report is $(describe_accept_status "${accept_status}")")
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
  # the same run just published, until wall time caught up. An unparseable or
  # zone-less stamp prints "unreadable" from the helper above and is still a
  # problem.
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

if [ ${#unusable[@]} -gt 0 ]; then
  echo "Ports whose newest report was unusable (an older one is still being served):" >&2
  printf '  %s\n' "${unusable[@]}" >&2
  echo "The table is current, but the producer is emitting reports the website cannot read." >&2
  exit 1
fi

echo "Every port in the contract has a report inside the ${stale_days}-day window."

# Freshness says the port reported; it does not say the port reported on every
# test. That obligation used to be enforced against the checked-in fallbacks,
# where a branch could satisfy it by typing "pass" -- so it is enforced here
# instead, against what the ports actually published, where nothing anyone
# writes in a pull request can reach it. A registered test runs on every port,
# and the only permitted exception is a skip the suite itself emits with an
# erratum explaining it.
python3 "${SCRIPT_DIR}/port_status.py" coverage --reports "${published_dir}"
