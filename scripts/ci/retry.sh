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
# The delay DOUBLES after each failure (30s, 60s, 120s ...). A 403 from Central is
# a rate-limit response, not a blip: it keeps rejecting the runner's IP for as long
# as the throttle holds, so a flat delay just spends every attempt inside the same
# outage window. The errorprone job burned all three 30s-apart attempts that way.
# Backing off costs nothing when the command succeeds, and only lengthens the tail
# of a run that was going to fail anyway.
#
# RETRY_ONLY_MATCHING is an extended regex that narrows retrying to failures
# whose output matches it -- for steps that RUN TESTS, where a blanket retry
# would quietly convert a flaky test into a pass and hide exactly the kind of
# race this repo requires to be root-caused. With it set, a failure that does not
# match is returned on the first attempt, so only the transient-resolution case
# gets a second chance. Leave it unset for steps that merely download and build.
attempts="${RETRY_ATTEMPTS:-3}"
delay="${RETRY_DELAY_SECONDS:-30}"
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
      "retrying in ${delay}s (possible transient Maven Central 403/429/5xx)" >&2
    sleep "$delay"
    delay=$((delay * 2))
  fi
  attempt=$((attempt + 1))
done

echo "retry.sh: command failed after ${attempts} attempts" >&2
exit "$status"
