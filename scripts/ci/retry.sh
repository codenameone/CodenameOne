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
attempts="${RETRY_ATTEMPTS:-3}"
delay="${RETRY_DELAY_SECONDS:-30}"

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
while [ "$attempt" -le "$attempts" ]; do
  "$@" && exit 0
  status=$?
  if [ "$attempt" -lt "$attempts" ]; then
    echo "retry.sh: attempt ${attempt}/${attempts} failed with status ${status};" \
      "retrying in ${delay}s (possible transient Maven Central 403/429/5xx)" >&2
    sleep "$delay"
  fi
  attempt=$((attempt + 1))
done

echo "retry.sh: command failed after ${attempts} attempts" >&2
exit "$status"
