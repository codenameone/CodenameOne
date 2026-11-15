#!/usr/bin/env bash
# The two halves of .github/workflows/golden-harvest.yml; see that file.
#
#   golden-harvest.sh list SHA RUNS_FILE    the screenshot runs at SHA; sets
#                                           ready=1 in GITHUB_OUTPUT when all completed
#   golden-harvest.sh collect RUNS_FILE DEST   take their unowned-golden images into DEST
#
# Needs GH_TOKEN (actions: read) and GITHUB_REPOSITORY.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
# Read from golden-capture.sh so the two lists cannot drift.
mapfile -t WORKFLOWS < <(sed -n '/^WORKFLOWS=(/,/^)/{/^  /s/^  //p;}' "$HERE/golden-capture.sh")

log() { echo "[golden-harvest] $*" >&2; }

list() {
  local sha="$1" runs_file="$2" wf line pending=0
  [ "${#WORKFLOWS[@]}" -gt 0 ] || { log "no workflows read from golden-capture.sh"; return 1; }
  : > "$runs_file"
  for wf in "${WORKFLOWS[@]}"; do
    while read -r line; do
      [ -n "$line" ] || continue
      set -- $line
      if [ "$2" != completed ]; then
        log "$wf run $1 is $2"
        pending=1
      fi
      printf '%s %s\n' "$wf" "$1" >> "$runs_file"
    done < <(gh api "repos/$GITHUB_REPOSITORY/actions/workflows/$wf/runs?head_sha=$sha&event=pull_request&per_page=20" \
               --jq '.workflow_runs[] | "\(.id) \(.status)"')
  done
  if [ "$pending" = 0 ] && [ -s "$runs_file" ]; then
    echo "ready=1" >> "$GITHUB_OUTPUT"
  else
    log "not every screenshot run at $sha has completed; the last one harvests"
    echo "ready=0" >> "$GITHUB_OUTPUT"
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

case "${1:-}" in
  list) list "$2" "$3" ;;
  collect) collect "$2" "$3" ;;
  *) echo "usage: $0 list|collect ..." >&2; exit 2 ;;
esac
