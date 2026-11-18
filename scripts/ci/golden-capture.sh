#!/usr/bin/env bash
# Dispatch the screenshot workflows of a golden capture. Called by
# .github/workflows/golden-capture.yml; see that file for the model.
#
#   golden-capture.sh dispatch REF TESTS RUNS_FILE [WF...]  start the workflows, record run ids
#   golden-capture.sh workflows DIR...                    the workflows that render those golden dirs
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

# The workflows that own a hellocodenameone golden directory.
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

# The workflows whose legs compare against each golden directory (the
# SCREENSHOT_REF_DIR / REF values in them; DIR_PORTS in cn1ss-golden-plan.py).
# A directory not listed here takes every workflow: capturing too much is only
# slower, capturing too little never harvests.
workflows() {
  local d want=""
  for d in "$@"; do
    case "$d" in
      scripts/ios/*) want+=" scripts-ios.yml" ;;
      scripts/android/*) want+=" scripts-android.yml" ;;
      scripts/javascript/*) want+=" scripts-javascript.yml" ;;
      scripts/linux/*) want+=" linux-build-run.yml" ;;
      scripts/windows/*) want+=" parparvm-tests-windows.yml windows-cross-build-run.yml" ;;
      scripts/macos/*) want+=" scripts-macos.yml" ;;
      *) want+=" ${WORKFLOWS[*]}" ;;
    esac
  done
  for d in "${WORKFLOWS[@]}"; do
    case " $want " in *" $d "*) echo "$d" ;; esac
  done
}

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
  local ref="$1" tests="$2" runs_file="$3" wf since seen id i deadline missing
  shift 3
  local targets=("$@")
  [ "${#targets[@]}" -gt 0 ] || targets=("${WORKFLOWS[@]}")
  : > "$runs_file"
  local -a sinces=() seens=()
  for wf in "${targets[@]}"; do
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
    sinces+=("$since")
    seens+=("$seen")
  done
  # The dispatch API answers without a run id, and GitHub can take many
  # minutes to list a run it has already created (a run created at 18:20:05
  # was still unlisted five minutes later and failed a whole capture). So every
  # workflow is dispatched first, then all of them are looked up together
  # against ONE generous deadline: a slow listing costs time, never the capture,
  # and never holds up finding the others. CN1SS_FIND_RUN_MINUTES overrides it.
  deadline=$(( $(date +%s) + ${CN1SS_FIND_RUN_MINUTES:-40} * 60 ))
  local -a ids=()
  while :; do
    missing=0
    for i in "${!targets[@]}"; do
      [ -n "${ids[$i]:-}" ] && continue
      wf="${targets[$i]}"
      id="$(gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
              --event workflow_dispatch --limit 20 --json databaseId,createdAt \
              --jq "[.[] | select(.createdAt >= \"${sinces[$i]}\") | select(.databaseId | tostring | IN(\"${seens[$i]}\" | split(\",\")[]) | not)] | .[0].databaseId // empty" \
              2>/dev/null || true)"
      if [ -n "$id" ]; then
        ids[$i]="$id"
        log "$wf -> run $id"
      else
        missing=$((missing + 1))
      fi
    done
    [ "$missing" -eq 0 ] && break
    if [ "$(date +%s)" -ge "$deadline" ]; then
      for i in "${!targets[@]}"; do
        [ -n "${ids[$i]:-}" ] || log "no run of ${targets[$i]} appeared after dispatch"
      done
      # Record what did start, so the failure step cancels those runs.
      for i in "${!targets[@]}"; do
        [ -n "${ids[$i]:-}" ] && printf '%s %s\n' "${targets[$i]}" "${ids[$i]}" >> "$runs_file"
      done
      return 1
    fi
    sleep 20
  done
  for i in "${!targets[@]}"; do
    printf '%s %s\n' "${targets[$i]}" "${ids[$i]}" >> "$runs_file"
  done
}

case "${1:-}" in
  dispatch) shift; dispatch "$@" ;;
  workflows) shift; workflows "$@" ;;
  *) echo "usage: $0 dispatch ..." >&2; exit 2 ;;
esac
