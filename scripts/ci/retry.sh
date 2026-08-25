#!/usr/bin/env bash
set -uo pipefail

# Run a command, retrying on failure, for CI steps whose only realistic failure
# mode is a transient artifact-repository hiccup. Maven Central intermittently
# serves 403 or 429 ("Too Many Requests") from the runner CDN edge, and Maven
# treats either as a permanent resolution failure -- the build dies at
# dependency/plugin download, before anything is compiled. A real build or test
# failure fails identically on every attempt, so retrying costs only time.
#
# Usage: bash scripts/ci/retry.sh mvn -B -f vm/JavaAPI/pom.xml package
#
# RETRY_ATTEMPTS (default 3) and RETRY_DELAY_SECONDS (default 30) override the
# bounds; the command is always attempted at least once and the last attempt's
# exit status is what this script returns.
#
# RETRY_ONLY_MATCHING is an extended regex that narrows retrying to failures
# whose output matches it -- for steps that RUN TESTS, where a blanket retry
# would quietly convert a flaky test into a pass and hide exactly the kind of
# race this repo requires to be root-caused. With it set, a failure that does not
# match is returned on the first attempt, so only the transient-resolution case
# gets a second chance. Leave it unset for steps that merely download and build.
# Four, not three. With the growing wait below that is 30s, 2m and 5m of
# coverage -- the same 30/120/300 the Windows cross-compile workflow settled on.
# Three attempts spanned four minutes and still lost to a 429 window.
attempts="${RETRY_ATTEMPTS:-4}"
delay="${RETRY_DELAY_SECONDS:-30}"
# The wait GROWS. A flat retry spends all its attempts inside the same window
# Central is still refusing in -- observed as three 403s in sixty seconds, which
# failed the branch for a reason that had nothing to do with it. Quadrupling
# from the first delay gives 30s, 2m, 5m, which is the same shape the Windows
# cross-compile workflow settled on for the same reason.
max_delay="${RETRY_MAX_DELAY_SECONDS:-300}"
only_matching="${RETRY_ONLY_MATCHING:-}"

if [ "$#" -eq 0 ]; then
  echo "retry.sh: no command given" >&2
  exit 2
fi

# A malformed override must not silently degrade into "never ran the command,
# reported success" -- these are set in workflow YAML, so a typo should be loud.
# RETRY_ATTEMPTS must be >= 1: the command always runs at least once.
case "$attempts" in
  ''|*[!0-9]*|0) echo "retry.sh: RETRY_ATTEMPTS must be a positive integer, got '${attempts}'" >&2; exit 2 ;;
esac
case "$delay" in
  ''|*[!0-9]*) echo "retry.sh: RETRY_DELAY_SECONDS must be a non-negative integer, got '${delay}'" >&2; exit 2 ;;
esac
case "$max_delay" in
  ''|*[!0-9]*) echo "retry.sh: RETRY_MAX_DELAY_SECONDS must be a non-negative integer, got '${max_delay}'" >&2; exit 2 ;;
esac
if [ "$max_delay" -lt "$delay" ]; then
  max_delay="$delay"
fi
wait_seconds="$delay"

# Counted with arithmetic rather than `seq`: BSD and GNU seq disagree on
# degenerate ranges, and this loop body must run exactly `attempts` times.
status=0
attempt=1
log=""
if [ -n "$only_matching" ]; then
  log="$(mktemp)"
  trap 'rm -f "$log"' EXIT
fi
while [ "$attempt" -le "$attempts" ]; do
  if [ -n "$only_matching" ]; then
    # Streamed as well as captured, so the step's log reads exactly as it would
    # without the wrapper.
    "$@" 2>&1 | tee "$log"
    status=$?
  else
    "$@" && exit 0
    status=$?
  fi
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if [ -n "$only_matching" ] && ! grep -Eq "$only_matching" "$log"; then
    echo "retry.sh: attempt ${attempt}/${attempts} failed with status ${status} and the" \
      "output does not match RETRY_ONLY_MATCHING, so this is a real failure, not a" \
      "transient one -- not retrying" >&2
    exit "$status"
  fi
  if [ "$attempt" -lt "$attempts" ]; then
    echo "retry.sh: attempt ${attempt}/${attempts} failed with status ${status};" \
      "retrying in ${wait_seconds}s (possible transient Maven Central 403/429/5xx)" >&2
    sleep "$wait_seconds"
    wait_seconds=$((wait_seconds * 4))
    if [ "$wait_seconds" -gt "$max_delay" ]; then
      wait_seconds="$max_delay"
    fi
  fi
  attempt=$((attempt + 1))
done

echo "retry.sh: command failed after ${attempts} attempts" >&2
exit "$status"
