#!/usr/bin/env bash
#
# Runs Maven, retrying when the failure looks like Maven Central rather than the build.
#
# Maven treats a connection reset or a throttled response while resolving a dependency as a
# PERMANENT resolution failure and stops. On a CI runner that is common enough to matter, and it
# happens before any project code compiles, so the job fails having proved nothing. Observed on
# PR CI as:
#
#   Failed to collect dependencies at org.apache.maven.plugins:maven-shade-plugin:jar:3.1.1
#   -> org.ow2.asm:asm:jar:6.0: ... transfer failed for .../asm-6.0.pom: Connection reset
#
# setup-workspace.sh has carried its own copy of this logic for exactly this reason; this is the
# same rationale and the same backoff, in a form a workflow step can call. The delay grows
# because a flat retry lands inside the window a rate limit is still in force. A genuinely broken
# build fails identically on every attempt, so this costs one extra run of a build that was
# already failing and rescues one that was not.
#
# Usage: scripts/ci-mvn-retry.sh <maven args...>
set -uo pipefail

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <maven args...>" >&2
  exit 2
fi

MVN_BIN="${MAVEN_HOME:+$MAVEN_HOME/bin/}mvn"

for delay in 30 120 300 0; do
  if "$MVN_BIN" "$@"; then
    exit 0
  fi
  if [ "$delay" = "0" ]; then
    echo "[ci-mvn-retry] maven failed after all retries" >&2
    exit 1
  fi
  echo "[ci-mvn-retry] maven failed; retrying in ${delay}s in case Maven Central was flaky" >&2
  sleep "$delay"
done
