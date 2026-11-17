#!/usr/bin/env bash
# Dispatch the screenshot workflows of a golden capture. Called by
# .github/workflows/golden-capture.yml; see that file for the model.
#
#   golden-capture.sh dispatch REF TESTS RUNS_FILE   start every workflow, record run ids
#
# Nothing here waits for the runs: golden-harvest.yml collects them when the
# last one completes, so no runner sits idle for the hours a slow leg takes.
#
# Needs GH_TOKEN (actions: write), DISPATCH_TOKEN (CN1SS_GH_TOKEN) and
# GITHUB_REPOSITORY. Each dispatch is a run
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

# A transient API error must not abandon a capture whose runs are fine.
gh_retry() {
  local i
  for i in 1 2 3 4 5; do
    "$@" && return 0
    sleep $((i * 15))
  done
  return 1
}

dispatch() {
  local ref="$1" tests="$2" runs_file="$3" wf since seen id tries
  : > "$runs_file"
  for wf in "${WORKFLOWS[@]}"; do
    # A capture of an earlier head of this branch is rendering code that is
    # gone; its harvest would be skipped anyway (the branch moved).
    for id in $(gh_retry gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
                  --event workflow_dispatch --limit 20 --json databaseId,status \
                  --jq '.[] | select(.status != "completed") | .databaseId'); do
      log "cancelling the earlier capture run $id of $wf"
      gh run cancel "$id" --repo "$GITHUB_REPOSITORY" || true
    done
    # A minute early: the runner's clock is not GitHub's. Runs that already
    # existed are excluded by id instead.
    since="$(date -u -d '-60 seconds' +%Y-%m-%dT%H:%M:%SZ)"
    seen="$(gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
              --event workflow_dispatch --limit 20 --json databaseId --jq '[.[].databaseId] | join(",")')"
    # DISPATCH_TOKEN (the push token): GitHub starts no workflow_run for a
    # run github.token started, and golden-harvest.yml is a workflow_run.
    if ! { [ -n "${DISPATCH_TOKEN:-}" ] &&
           GH_TOKEN="$DISPATCH_TOKEN" gh workflow run "$wf" --repo "$GITHUB_REPOSITORY" --ref "$ref" -f cn1ss_capture="$tests"; }; then
      echo "::warning::could not dispatch $wf with CN1SS_GH_TOKEN (unset, lacking Actions: write, or a transient error); dispatching with github.token, so the harvest will not start by itself: run Golden harvest by hand with head_ref=$ref once the capture completes"
      gh workflow run "$wf" --repo "$GITHUB_REPOSITORY" --ref "$ref" -f cn1ss_capture="$tests"
    fi
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

case "${1:-}" in
  dispatch) dispatch "$2" "$3" "$4" ;;
  *) echo "usage: $0 dispatch ..." >&2; exit 2 ;;
esac
