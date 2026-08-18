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
# not something a wrapper grinds away at.
#
# Two rules keep that honest, and the second exists because the first was not enough. A compile
# or test failure anywhere in the log vetoes the retry outright. Otherwise the decision is made
# on the *terminal* failure -- the part after the last BUILD FAILURE -- rather than on any
# matching text in the whole log, because Maven retries some transfers internally and a warning
# it recovered from can share a log with a real failure. Matching anywhere would rerun that.
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

# Markers that mean "the transport misbehaved", never "your build is wrong".
#
# Deliberately narrow, and narrower than it used to be. It matched any 4xx and the generic
# resolution phrases, so a POM naming an artifact or version that does not exist -- a 404, and
# "Could not resolve dependencies" -- was read as transient and reran three times. That is 7.5
# minutes added to a deterministic configuration error before anyone sees it, from a script
# whose whole promise is that it does not retry real failures.
#
# The resolution phrases are gone entirely, because they are symptoms rather than causes: Maven
# prints them for a missing artifact and for a throttled one alike, and when the cause really is
# the transport it says so on the same line ("Non-resolvable import POM: ... Could not transfer
# artifact ...: status code: 429"). So the cause is what gets matched.
#
# 403 stays, against the general rule that 4xx is permanent, because this repository has
# watched Central answer 403 to a whole CI matrix under load -- it is in the header above, from
# the day this script was written. 404 does not stay: nothing makes a missing artifact appear.
TRANSIENT='Connection reset|Premature EOF|Connection timed out|Read timed out|Too Many Requests|Bad Gateway|Service Unavailable|Gateway Time-?out|status: 429|status code: 429|status: 403|status code: 403|status: 5[0-9][0-9]|status code: 5[0-9][0-9]'

# Markers that mean the build itself failed. Any of these vetoes a retry outright, however the
# log started: Maven retries some transfers internally, so an early warning that it recovered
# from can sit in the same log as a genuine compile or test failure. Matching anywhere would
# then rerun that failure -- and a flaky test passing on attempt three is precisely the outcome
# this wrapper must never produce.
DEFINITE='COMPILATION ERROR|There are test failures|Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]|BUILD FAILURE.*\n.*Compilation failure'

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

for delay in 30 120 300 0; do
  # tee so the step's own output is preserved verbatim; PIPESTATUS keeps Maven's exit code.
  "$MVN_BIN" "$@" 2>&1 | tee "$log"
  status="${PIPESTATUS[0]}"
  if [ "$status" -eq 0 ]; then
    exit 0
  fi

  if grep -Eq "$DEFINITE" "$log"; then
    echo "[ci-mvn-retry] the build failed on its own merits; not retrying" >&2
    exit "$status"
  fi

  # Classify on the failure Maven actually stopped for, not on anything it printed along the
  # way. Everything from the last "BUILD FAILURE" (or the last failing goal) to the end is the
  # part that explains the exit code.
  terminal="$(awk '/BUILD FAILURE|Failed to execute goal/ { buf = "" } { buf = buf $0 "\n" } END { print buf }' "$log")"
  if [ -z "$terminal" ]; then
    terminal="$(tail -50 "$log")"
  fi

  if ! printf '%s' "$terminal" | grep -Eq "$TRANSIENT"; then
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
