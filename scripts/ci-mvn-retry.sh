#!/usr/bin/env bash
#
# Runs Maven, retrying only when the failure looks like Maven Central rather than the build.
#
# Maven treats a failed artifact transfer as permanent and stops, so a runner that gets a 429,
# a 502 or a reset connection while resolving a build plugin fails the job in its setup step,
# before any project code compiles. Observed repeatedly on PR CI in one day:
#
#   ... transfer failed for .../asm-6.0.pom: Connection reset
#   ... central-publishing-maven-plugin ... status: 403 Forbidden
#   ... plexus-xml:jar:3.0.1 ... status code: 502, reason phrase: Bad Gateway
#   ... junit-bom:pom:5.9.3 ... status: 429
#
# **The retry is conditional on purpose.** Re-running any failed Maven command would retry a
# genuine compile or test failure three more times -- slow, and worse, it could let a flaky test
# pass on a later attempt and report green. A red build here is meant to be a bug someone fixes,
# not something a wrapper grinds away at. So the output is inspected, and anything that is not
# recognisably a transport failure fails immediately with its original exit code.
#
# The backoff grows because a flat retry lands inside the window a rate limit is still in force.
#
# Usage: scripts/ci-mvn-retry.sh <maven args...>
set -uo pipefail

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <maven args...>" >&2
  exit 2
fi

MVN_BIN="${MAVEN_HOME:+$MAVEN_HOME/bin/}mvn"

# Markers that mean "the network or the repository misbehaved", never "your code is wrong".
TRANSIENT='Could not transfer artifact|Unresolveable build extension|Failed to read artifact descriptor|Non-resolvable import POM|Could not resolve dependencies|Connection reset|Premature EOF|Connection timed out|status: 4[0-9][0-9]|status code: 5[0-9][0-9]|Bad Gateway|Service Unavailable|Too Many Requests'

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

for delay in 30 120 300 0; do
  # tee so the step's own output is preserved verbatim; PIPESTATUS keeps Maven's exit code.
  "$MVN_BIN" "$@" 2>&1 | tee "$log"
  status="${PIPESTATUS[0]}"
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if ! grep -Eq "$TRANSIENT" "$log"; then
    echo "[ci-mvn-retry] failure does not look like a repository problem; not retrying" >&2
    exit "$status"
  fi
  if [ "$delay" = "0" ]; then
    echo "[ci-mvn-retry] maven failed after all retries" >&2
    exit "$status"
  fi
  echo "[ci-mvn-retry] repository transfer failed; retrying in ${delay}s" >&2
  sleep "$delay"
done
