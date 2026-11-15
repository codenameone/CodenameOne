#!/usr/bin/env bash
# Dispatch, wait for, and collect the screenshot workflows of a golden capture.
# Called by .github/workflows/golden-capture.yml; see that file for the model.
#
#   golden-capture.sh dispatch REF TESTS RUNS_FILE   start every workflow, record run ids
#   golden-capture.sh wait RUNS_FILE                 block until all have completed
#   golden-capture.sh collect RUNS_FILE DEST [BASE]  merge cn1ss-capture-* artifacts into DEST
#
# Needs GH_TOKEN (actions: write) and GITHUB_REPOSITORY. Each dispatch is a run
# of its own, so the workflows' concurrency groups and fixed artifact names
# (the Unity gallery, the port bundles) cannot collide as they would if they
# were called inside one run.
set -euo pipefail

# The workflows that own a hellocodenameone golden directory. scripts-ios.yml
# also carries Mac Catalyst (it passes the input on to that call).
WORKFLOWS=(
  scripts-ios.yml
  scripts-android.yml
  scripts-javascript.yml
  linux-build-run.yml
  parparvm-tests-windows.yml
  windows-cross-build-run.yml
  scripts-macos.yml
)

log() { echo "[golden-capture] $*" >&2; }

dispatch() {
  local ref="$1" tests="$2" runs_file="$3" wf since seen id tries
  : > "$runs_file"
  for wf in "${WORKFLOWS[@]}"; do
    # A minute early: the runner's clock is not GitHub's. Runs that already
    # existed are excluded by id instead.
    since="$(date -u -d '-60 seconds' +%Y-%m-%dT%H:%M:%SZ)"
    seen="$(gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
              --event workflow_dispatch --limit 20 --json databaseId --jq '[.[].databaseId] | join(",")')"
    gh workflow run "$wf" --repo "$GITHUB_REPOSITORY" --ref "$ref" -f cn1ss_capture="$tests"
    # The dispatch API answers without a run id; find the run it created.
    id=""
    for tries in $(seq 1 30); do
      id="$(gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
              --event workflow_dispatch --limit 5 --json databaseId,createdAt \
              --jq "[.[] | select(.createdAt >= \"$since\") | select(.databaseId | tostring | IN(\"$seen\" | split(\",\")[]) | not)] | .[0].databaseId // empty")"
      [ -n "$id" ] && break
      sleep 10
    done
    if [ -z "$id" ]; then
      log "no run of $wf appeared after dispatch"
      return 1
    fi
    log "$wf -> run $id"
    printf '%s %s\n' "$wf" "$id" >> "$runs_file"
  done
}

wait_all() {
  local runs_file="$1" wf id pending status
  while :; do
    pending=0
    while read -r wf id; do
      status="$(gh run view "$id" --repo "$GITHUB_REPOSITORY" --json status --jq .status)"
      [ "$status" = "completed" ] || pending=$((pending + 1))
    done < "$runs_file"
    [ "$pending" -eq 0 ] && break
    log "$pending run(s) still going"
    sleep 120
  done
}

collect() {
  local runs_file="$1" dest="$2" base="${3:-}" wf id conclusion failed=0 dl
  dl="$(mktemp -d)"
  while read -r wf id; do
    conclusion="$(gh run view "$id" --repo "$GITHUB_REPOSITORY" --json conclusion --jq .conclusion)"
    log "$wf run $id: $conclusion"
    [ "$conclusion" = "success" ] || failed=1
    gh run download "$id" --repo "$GITHUB_REPOSITORY" -p 'cn1ss-capture-*' -D "$dl/$id" 2>/dev/null \
      || log "$wf run $id uploaded no capture"
  done < "$runs_file"
  # Each artifact holds goldens at their repository paths; only images that
  # fill a gap the detector finds are taken (see golden-capture-collect.py).
  python3 "$(dirname "$0")/golden-capture-collect.py" "$dl" "$dest" ${base:+"$base"} || failed=1
  return "$failed"
}

case "${1:-}" in
  dispatch) dispatch "$2" "$3" "$4" ;;
  wait) wait_all "$2" ;;
  collect) collect "$2" "$3" "${4:-}" ;;
  *) echo "usage: $0 dispatch|wait|collect ..." >&2; exit 2 ;;
esac
