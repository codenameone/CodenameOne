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

status=0
for attempt in $(seq 1 "$attempts"); do
  "$@" && exit 0
  status=$?
  if [ "$attempt" -lt "$attempts" ]; then
    echo "retry.sh: attempt ${attempt}/${attempts} failed with status ${status};" \
      "retrying in ${delay}s (possible transient Maven Central 403/429/5xx)" >&2
    sleep "$delay"
  fi
done

echo "retry.sh: command failed after ${attempts} attempts" >&2
exit "$status"
