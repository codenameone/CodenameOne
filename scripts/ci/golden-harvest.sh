#!/usr/bin/env bash
# The two halves of .github/workflows/golden-harvest.yml; see that file.
#
#   golden-harvest.sh list SHA RUNS_FILE    the newest uncancelled run of each screenshot workflow at SHA; sets
#                                           ready=1 in GITHUB_OUTPUT when all completed
#   golden-harvest.sh count SHA             how many screenshot runs exist at SHA
#   golden-harvest.sh collect RUNS_FILE DEST   take their unowned-golden images into DEST
#   golden-harvest.sh rerun SHA             re-run the newest cancelled pull_request run of each
#                                           screenshot workflow at SHA (a capture that committed nothing)
#
# A run is a pull_request run (a normal PR run, harvest mode) or a
# workflow_dispatch run (golden-capture.yml's capture mode); both are listed,
# and the newest uncancelled one of each workflow is the one that counts.
#
# Needs GH_TOKEN (actions: read; write for rerun) and GITHUB_REPOSITORY.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
# Read from golden-capture.sh so the two lists cannot drift.
mapfile -t WORKFLOWS < <(sed -n '/^WORKFLOWS=(/,/^)/{/^  /s/^  //p;}' "$HERE/golden-capture.sh")

log() { echo "[golden-harvest] $*" >&2; }

# Every run of workflow $1 at SHA $2 from a PR or a capture, as a JSON array.
runs_at() {
  gh api "repos/$GITHUB_REPOSITORY/actions/workflows/$1/runs?head_sha=$2&per_page=50" \
    --jq '[.workflow_runs[] | select(.event == "pull_request" or .event == "workflow_dispatch")]'
}

list() {
  local sha="$1" runs_file="$2" wf line runs pending=0 capture=0 all
  [ "${#WORKFLOWS[@]}" -gt 0 ] || { log "no workflows read from golden-capture.sh"; return 1; }
  : > "$runs_file"
  # A capture (golden-capture.yml) dispatches the workflows one after another
  # over a few minutes. Once any of them has a live capture run here, a
  # workflow without one is still to come, not finished: harvesting then would
  # take the legs that are done and leave the rest to a branch that has moved.
  # Cancelled capture runs do not count: a capture that failed part way
  # cancels the ones it started and re-runs the PR's runs, which then harvest
  # as usual. (A screenshot workflow dispatched by hand at a PR head counts as
  # a capture too; nothing else dispatches them.) A capture that failed after
  # one of its legs had already completed leaves that run live and the head
  # never harvests; the PR's re-run golden-plan still names the gap, and the
  # next push captures again. Legs run far longer than dispatching takes.
  all=""
  for wf in "${WORKFLOWS[@]}"; do
    # One assignment per workflow, so a failed API call stops the listing
    # under set -e instead of dropping that workflow from it.
    runs="$(runs_at "$wf" "$sha")"
    all+="$(jq -c --arg wf "$wf" '{wf: $wf, runs: .}' <<<"$runs")"$'\n'
  done
  if jq -e -s 'any(.[].runs[]; .event == "workflow_dispatch" and .conclusion != "cancelled")' <<<"$all" >/dev/null; then
    capture=1
  fi
  for wf in "${WORKFLOWS[@]}"; do
    runs="$(jq -c -s --arg wf "$wf" '.[] | select(.wf == $wf) | .runs' <<<"$all")"
    if [ "$capture" = 1 ] && ! jq -e 'any(.[]; .event == "workflow_dispatch" and .conclusion != "cancelled")' <<<"$runs" >/dev/null; then
      log "$wf has no capture run at $sha yet"
      pending=1
      continue
    fi
    # A workflow whose every run here was cancelled has nothing to harvest;
    # say so, since its suite never reported.
    if [ "$(jq '(length > 0 and all(.conclusion == "cancelled"))' <<<"$runs")" = true ]; then
      echo "::warning::every $wf run at $sha was cancelled; nothing of it is harvested"
    fi
    while read -r line; do
      [ -n "$line" ] || continue
      set -- $line
      if [ "$2" != completed ]; then
        log "$wf run $1 is $2"
        pending=1
      fi
      printf '%s %s\n' "$wf" "$1" >> "$runs_file"
    done < <(jq -r '[.[] | select(.conclusion != "cancelled")] | sort_by(.id) | last // empty | "\(.id) \(.status)"' <<<"$runs")
  done
  echo "capture=$capture" >> "${GITHUB_OUTPUT:-/dev/null}"
  if [ "$pending" = 0 ] && [ -s "$runs_file" ]; then
    echo "ready=1" >> "${GITHUB_OUTPUT:-/dev/null}"
  else
    log "not every screenshot run at $sha has completed; the last one harvests"
    echo "ready=0" >> "${GITHUB_OUTPUT:-/dev/null}"
    [ -z "${HARVEST_REQUIRE_READY:-}" ] || return 3
  fi
}

collect() {
  local runs_file="$1" dest="$2" wf id dl
  dl="$(mktemp -d)"
  while read -r wf id; do
    gh run download "$id" --repo "$GITHUB_REPOSITORY" -p 'cn1ss-capture-*' -D "$dl/$id" 2>/dev/null \
      || log "$wf run $id uploaded nothing to harvest"
  done < "$runs_file"
  mkdir -p "$dl"
  # The PR's own mappings: a new variant comes with the entry that owns it.
  python3 -I "$HERE/golden-capture-collect.py" --harvest \
    "--manifest=$dest/docs/website/data/port_status.json" "$dl" "$dest"
}

# How many screenshot runs exist at SHA, any status.
count() {
  local sha="$1" wf n total=0
  for wf in "${WORKFLOWS[@]}"; do
    n="$(runs_at "$wf" "$sha" | jq length)"
    total=$((total + n))
  done
  echo "$total"
}

# A capture cancelled the PR's own runs, screenshot workflows and the rest
# (cancel-stale-pr-runs.js, cancelForCapture); when it brings nothing back,
# run them again, so golden-plan names the open gap on the PR instead of
# leaving its checks cancelled: the newest pull_request run of each workflow
# at SHA, when that run was cancelled.
rerun() {
  local sha="$1" id
  for id in $(gh api --paginate "repos/$GITHUB_REPOSITORY/actions/runs?head_sha=$sha&event=pull_request&per_page=100" \
                --jq '.workflow_runs[] | [.workflow_id, .id, .conclusion] | @tsv' \
              | sort -k1,1n -k2,2n | awk -F'\t' '{last[$1] = $2 "\t" $3} END {for (w in last) print last[w]}' \
              | awk -F'\t' '$2 == "cancelled" {print $1}'); do
    log "re-running run $id"
    gh run rerun "$id" --repo "$GITHUB_REPOSITORY" || echo "::warning::could not re-run run $id"
  done
}

case "${1:-}" in
  count) count "$2" ;;
  list) list "$2" "$3" ;;
  collect) collect "$2" "$3" ;;
  rerun) rerun "$2" ;;
  *) echo "usage: $0 list|count|collect|rerun ..." >&2; exit 2 ;;
esac
