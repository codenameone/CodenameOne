#!/usr/bin/env bash
# Dispatch, wait for, and collect the screenshot workflows of a golden capture.
# Called by .github/workflows/golden-capture.yml; see that file for the model.
#
#   golden-capture.sh dispatch REF TESTS RUNS_FILE   start every workflow, record run ids
#   golden-capture.sh wait RUNS_FILE                 block until all have completed
#   golden-capture.sh collect RUNS_FILE DEST         merge cn1ss-capture-* artifacts into DEST
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
  scripts-macos.yml
)

log() { echo "[golden-capture] $*" >&2; }

dispatch() {
  local ref="$1" tests="$2" runs_file="$3" wf since id tries
  : > "$runs_file"
  for wf in "${WORKFLOWS[@]}"; do
    since="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    gh workflow run "$wf" --repo "$GITHUB_REPOSITORY" --ref "$ref" -f cn1ss_capture="$tests"
    # The dispatch API answers without a run id; find the run it created.
    id=""
    for tries in $(seq 1 30); do
      id="$(gh run list --repo "$GITHUB_REPOSITORY" --workflow "$wf" --branch "$ref" \
              --event workflow_dispatch --limit 5 --json databaseId,createdAt \
              --jq "[.[] | select(.createdAt >= \"$since\")] | .[0].databaseId // empty")"
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
  local runs_file="$1" dest="$2" wf id conclusion failed=0 dl
  dl="$(mktemp -d)"
  while read -r wf id; do
    conclusion="$(gh run view "$id" --repo "$GITHUB_REPOSITORY" --json conclusion --jq .conclusion)"
    log "$wf run $id: $conclusion"
    [ "$conclusion" = "success" ] || failed=1
    gh run download "$id" --repo "$GITHUB_REPOSITORY" -p 'cn1ss-capture-*' -D "$dl/$id" 2>/dev/null \
      || log "$wf run $id uploaded no capture"
  done < "$runs_file"
  # Each artifact holds goldens at their repository paths. Never overwrite a
  # golden the branch already has, and keep the first capture when two legs
  # share a directory (Windows x64 and arm64 do).
  python3 - "$dl" "$dest" <<'PY'
import os, shutil, sys
dl, dest = sys.argv[1], sys.argv[2]
for run in sorted(os.listdir(dl)):
    for art in sorted(os.listdir(os.path.join(dl, run))):
        base = os.path.join(dl, run, art)
        for d, _, files in os.walk(base):
            for f in sorted(files):
                rel = os.path.relpath(os.path.join(d, f), base)
                target = os.path.join(dest, rel)
                if os.path.exists(target):
                    print("[golden-capture] kept existing %s (%s)" % (rel, art), file=sys.stderr)
                    continue
                os.makedirs(os.path.dirname(target), exist_ok=True)
                shutil.copyfile(os.path.join(d, f), target)
                print("[golden-capture] added %s (%s)" % (rel, art), file=sys.stderr)
PY
  return "$failed"
}

case "${1:-}" in
  dispatch) dispatch "$2" "$3" "$4" ;;
  wait) wait_all "$2" ;;
  collect) collect "$2" "$3" ;;
  *) echo "usage: $0 dispatch|wait|collect ..." >&2; exit 2 ;;
esac
