#!/usr/bin/env bash
# The two halves of .github/workflows/golden-harvest.yml; see that file.
#
#   golden-harvest.sh list SHA RUNS_FILE    the newest uncancelled run of each screenshot workflow at SHA; sets
#                                           ready=1 in GITHUB_OUTPUT when all completed
#   golden-harvest.sh count SHA             how many screenshot runs exist at SHA
#   golden-harvest.sh collect RUNS_FILE DEST   take their unowned-golden images into DEST
#   golden-harvest.sh status SHA STATE DESC set the "Golden capture" commit status on SHA,
#                                           and the PR's Golden capture comment
#   golden-harvest.sh comment SHA STATE DESC the comment alone
#   golden-harvest.sh redo-dropped RUNS_FILE  re-run the failed jobs of every listed run that
#                                           GitHub never gave a runner; prints how many runs
#
# A run is a pull_request run (a normal PR run, harvest mode) or a
# workflow_dispatch run (golden-capture.yml's capture mode); both are listed,
# and the newest uncancelled one of each workflow is the one that counts.
#
# Needs GH_TOKEN (actions: read; statuses: write for status) and GITHUB_REPOSITORY.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
# Read from golden-capture.sh so the two lists cannot drift.
mapfile -t WORKFLOWS < <(sed -n '/^WORKFLOWS=(/,/^)/{/^  /s/^  //p;}' "$HERE/golden-capture.sh")
# A pull request's own runs of those workflows are reusable calls inside
# ci.yml now, so its runs are where a normal PR run's capture artifacts are.
# Listed here only: golden-capture.sh dispatches the members, never ci.yml.
WORKFLOWS+=(ci.yml)

log() { echo "[golden-harvest] $*" >&2; }

# Every run of workflow $1 at SHA $2 from a PR or a capture, as a JSON array.
runs_at() {
  gh api "repos/$GITHUB_REPOSITORY/actions/workflows/$1/runs?head_sha=$2&per_page=50" \
    --jq '[.workflow_runs[] | select(.event == "pull_request" or .event == "workflow_dispatch")]'
}

list() {
  local sha="$1" runs_file="$2" wf line runs pending=0 completed=0 capture=0 all dispatched=false
  [ "${#WORKFLOWS[@]}" -gt 0 ] || { log "no workflows read from golden-capture.sh"; return 1; }
  : > "$runs_file"
  # A capture (golden-capture.yml) dispatches the workflows one after another
  # over a few minutes. A workflow without a run may still be coming until the
  # capture status says dispatching is complete. Outside a capture every
  # completed run is collected as it finishes; in one, the yml commits only
  # once every leg is done, since its push restarts the capture and the
  # restart cancels the legs still rendering.
  # A cancelled capture contributes no run unless the cancelled run itself
  # has a capture artifact; completed runs are still harvested regardless of
  # the state of other legs. (A screenshot workflow dispatched by hand at a PR
  # head counts as a capture too; nothing else dispatches them.)
  all=""
  for wf in "${WORKFLOWS[@]}"; do
    # One assignment per workflow, so a failed API call stops the listing
    # under set -e instead of dropping that workflow from it.
    runs="$(runs_at "$wf" "$sha")"
    all+="$(jq -c --arg wf "$wf" '{wf: $wf, runs: .}' <<<"$runs")"$'\n'
  done
  if jq -e -s 'any(.[].runs[]; .event == "workflow_dispatch" and .conclusion != "cancelled")' <<<"$all" >/dev/null; then
    capture=1
    # A capture starts only the workflows whose goldens are missing. Before
    # its status is set, a workflow without a capture run may still be coming.
    dispatched="$(gh api "repos/$GITHUB_REPOSITORY/commits/$sha/statuses" \
                    --jq 'any(.[]; .context == "Golden capture")')"
  fi
  for wf in "${WORKFLOWS[@]}"; do
    runs="$(jq -c -s --arg wf "$wf" '.[] | select(.wf == $wf) | .runs' <<<"$all")"
    if [ "$capture" = 1 ] && ! jq -e 'any(.[]; .event == "workflow_dispatch" and .conclusion != "cancelled")' <<<"$runs" >/dev/null; then
      if [ "$dispatched" = true ]; then
        log "$wf is not part of the capture at $sha"
      else
        log "$wf has no capture run at $sha yet"
        pending=1
      fi
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
      else
        completed=$((completed + 1))
      fi
      printf '%s %s\n' "$wf" "$1" >> "$runs_file"
    # In a capture, its own run: a PR run made later at the same head (the
    # PR closed and reopened) rendered nothing, its screenshot jobs skipped.
    done < <(jq -r --argjson capture "$capture" '[.[] | select(.conclusion != "cancelled")
                 | select($capture == 0 or .event == "workflow_dispatch")] | sort_by(.id) | last // empty
                 | "\(.id) \(.status)"' <<<"$runs")
  done
  echo "capture=$capture" >> "${GITHUB_OUTPUT:-/dev/null}"
  # counted as listed: the runs file holds only "workflow id", no status
  echo "runs=$completed" >> "${GITHUB_OUTPUT:-/dev/null}"
  if [ "$pending" = 0 ] && [ -s "$runs_file" ]; then
    echo "ready=1" >> "${GITHUB_OUTPUT:-/dev/null}"
  else
    log "some screenshot runs at $sha are still pending"
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

# The "Golden capture" commit status golden-capture.yml left pending on SHA:
# the PR's one view of a capture, whose runs (workflow_dispatch) GitHub does
# not show on the PR.
status() {
  local sha="$1" state="$2" desc="$3"
  gh api -X POST "repos/$GITHUB_REPOSITORY/statuses/$sha" -f state="$state" -f context="Golden capture" \
    -f description="${desc:0:140}" -f target_url="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID" >/dev/null \
    || echo "::warning::could not set the Golden capture status on $sha"
  comment "$sha" "$state" "$desc"
}

# The same news as one comment on the PR, edited in place: a commit status sits
# at the bottom of the checks, under a wall of cancelled ones, and is easy to
# miss. Posted by github-actions[bot] with the job's own token.
comment() {
  local sha="$1" state="$2" desc="$3" pr id body head run
  run="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID"
  pr="$(gh api "repos/$GITHUB_REPOSITORY/commits/$sha/pulls" --jq '[.[] | select(.state == "open")][0].number // empty' 2>/dev/null)" || pr=""
  [ -n "$pr" ] || return 0
  case "$state" in
    pending) head="Golden capture in progress" ;;
    success) head="Golden capture finished" ;;
    *) head="Golden capture needs attention" ;;
  esac
  body="<!-- golden-capture -->
### $head

$desc"
  if [ "$state" = pending ]; then
    body="$body

This PR adds or changes screenshot tests whose goldens do not exist yet, so its other checks are cancelled: they are known to fail until the goldens are committed. The capture renders them, commits them to this branch as \"Harvest goldens rendered at ...\", and that push starts CI again. **Do not push meanwhile**: a push starts the capture over."
  elif [ "$state" = success ]; then
    body="$body

CI is running again on the commit with the goldens. Review the images in that commit like any other golden change."
  else
    body="$body

Whatever did arrive is committed; the next push captures only what is still missing. A test that cannot render on some port needs a \`// cn1ss-skip: <ports>\` declaration."
  fi
  body="$body

[Run]($run) at \`${sha:0:12}\`"
  id="$(gh api "repos/$GITHUB_REPOSITORY/issues/$pr/comments" --paginate \
          --jq '.[] | select(.user.login == "github-actions[bot]") | select(.body | startswith("<!-- golden-capture -->")) | .id' 2>/dev/null | tail -1)" || {
    # comments that cannot be listed may still hold ours: posting would add a second
    echo "::warning::could not list the comments of #$pr; the Golden capture comment is not updated" >&2
    return 0
  }
  if [ -n "$id" ]; then
    gh api -X PATCH "repos/$GITHUB_REPOSITORY/issues/comments/$id" -f body="$body" >/dev/null
  else
    gh api -X POST "repos/$GITHUB_REPOSITORY/issues/$pr/comments" -f body="$body" >/dev/null
  fi || echo "::warning::could not update the Golden capture comment on #$pr"
}

# GitHub fails a queued job it cannot place ("The job was not acquired by
# Runner of type hosted even after multiple attempts"), most often a macOS
# arm64 one while that pool is short. The leg never ran, so it rendered
# nothing, and harvesting would report a failed capture for what is only a
# wait. Re-run those jobs instead, at most twice per run. The re-run must use
# CN1SS_GH_TOKEN (RERUN_TOKEN): GitHub starts no workflow_run, and so no
# harvest, for a run (re-)started with the job's own token.
redo-dropped() {
  local runs_file="$1" wf id attempt jobs job dropped notes n=0
  while read -r wf id; do
    attempt="$(gh api "repos/$GITHUB_REPOSITORY/actions/runs/$id" --jq 'if .conclusion == "success" then 0 else .run_attempt end')" || continue
    [ "$attempt" -gt 0 ] && [ "$attempt" -lt 3 ] || continue
    jobs="$(gh api --paginate "repos/$GITHUB_REPOSITORY/actions/runs/$id/jobs?per_page=100" \
              --jq '.jobs[] | select(.conclusion == "failure" or .conclusion == "cancelled") | .id')" || continue
    dropped=0
    for job in $jobs; do
      # read whole first: grep -q quitting early would SIGPIPE gh and, under
      # pipefail, read as no match
      notes="$(gh api "repos/$GITHUB_REPOSITORY/check-runs/$job/annotations" --jq '.[].message' 2>/dev/null)" || notes=""
      if [[ "$notes" == *"not acquired by Runner"* ]]; then
        dropped=1
        break
      fi
    done
    [ "$dropped" = 1 ] || continue
    log "$wf run $id: GitHub dropped a job before it got a runner; re-running its failed jobs (attempt $((attempt + 1)))"
    GH_TOKEN="${RERUN_TOKEN:-$GH_TOKEN}" gh run rerun "$id" --repo "$GITHUB_REPOSITORY" --failed ||
      echo "::warning::could not re-run the dropped jobs of run $id; moving on" >&2
    n=$((n + 1))
  done < "$runs_file"
  echo "$n"
}

case "${1:-}" in
  count) count "$2" ;;
  list) list "$2" "$3" ;;
  collect) collect "$2" "$3" ;;
  status) status "$2" "$3" "$4" ;;
  comment) comment "$2" "$3" "$4" ;;
  redo-dropped) redo-dropped "$2" ;;
  *) echo "usage: $0 list|count|collect|status|comment|redo-dropped ..." >&2; exit 2 ;;
esac
