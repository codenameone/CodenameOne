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
  local sha="$1" runs_file="$2" wf line runs pending=0
  [ "${#WORKFLOWS[@]}" -gt 0 ] || { log "no workflows read from golden-capture.sh"; return 1; }
  : > "$runs_file"
  for wf in "${WORKFLOWS[@]}"; do
    # A workflow whose every run here was cancelled has nothing to harvest;
    # say so, since its suite never reported.
    runs="$(runs_at "$wf" "$sha")"
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
  if [ "$pending" = 0 ] && [ -s "$runs_file" ]; then
    echo "ready=1" >> "${GITHUB_OUTPUT:-/dev/null}"
  else
    log "not every screenshot run at $sha has completed; the last one harvests"
    echo "ready=0" >> "${GITHUB_OUTPUT:-/dev/null}"
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

# A capture cancelled the PR's own runs; when it brings nothing back, run them
# again, so golden-plan names the open gap on the PR instead of leaving its
# checks cancelled.
rerun() {
  local sha="$1" wf id
  for wf in "${WORKFLOWS[@]}"; do
    id="$(runs_at "$wf" "$sha" | jq -r '[.[] | select(.event == "pull_request")] | sort_by(.id) | last // empty
            | select(.conclusion == "cancelled") | .id')"
    [ -n "$id" ] || continue
    log "re-running $wf run $id"
    gh run rerun "$id" --repo "$GITHUB_REPOSITORY" || echo "::warning::could not re-run $wf run $id"
  done
}

case "${1:-}" in
  count) count "$2" ;;
  list) list "$2" "$3" ;;
  collect) collect "$2" "$3" ;;
  rerun) rerun "$2" ;;
  *) echo "usage: $0 list|count|collect|rerun ..." >&2; exit 2 ;;
esac
