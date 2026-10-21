#!/usr/bin/env bash
#
# Runs a full-stack sample's server tests against a real PostgreSQL or MySQL
# database.
#
#   scripts/ci/sample-server-tests-on-database.sh <sample-dir> [mvn args...]
#
# CN1_TEST_DATABASE_URL names the database, as the server reads it:
#
#   postgres://user:password@host:port/db?sslmode=disable
#   mysql://user:password@host:port/db?sslmode=disable
#
# The schema is emptied with psql or mysql, whichever the URL needs, so that
# client has to be installed. CN1_TEST_DATABASE_RESET replaces that with a
# command of your own -- a `podman exec` into the database container, say, on
# a machine without the client.
#
# The samples' own test runs use an in-memory SQLite database, and SQLite takes
# any declared column type for any value. A mapping that disagrees with its
# migration -- an unbounded String on a VARCHAR(64), an int on a SMALLINT -- is
# accepted there and refused by the ORM's schema validation everywhere else, so
# the server never started on PostgreSQL or MySQL while every test stayed green. This is
# the run that sees what a deployment sees.
#
# Every test class gets an empty schema. An in-memory database is new for each
# application the tests start, and the tests are written for that: their
# accounts and orders would collide with the ones the class before left behind
# in a database that outlives the run. That is a property of the tests, not a
# difference between the databases, so it is reset here rather than written around in them.
#
# The JVM runner only. The translated runner is the same code over the same
# driver protocol, and the SQLite leg already runs it.
set -euo pipefail

SAMPLE="${1:?usage: $0 <sample-dir> [mvn args...]}"
shift
URL="${CN1_TEST_DATABASE_URL:?set CN1_TEST_DATABASE_URL to a postgres:// or mysql:// URL}"
MVN="${MVN:-mvn}"

log() { echo "[database-tests] $*" >&2; }

reset_schema() {
  if [ -n "${CN1_TEST_DATABASE_RESET:-}" ]; then
    bash -c "$CN1_TEST_DATABASE_RESET" >/dev/null
    return
  fi
  case "$URL" in
    postgres://*|postgresql://*)
      psql "$URL" -q -v ON_ERROR_STOP=1 \
          -c "DROP SCHEMA IF EXISTS public CASCADE" -c "CREATE SCHEMA public" >/dev/null
      ;;
    mysql://*)
      # mysql takes no URL: mysql://user:password@host:port/db?options
      local rest="${URL#mysql://}" auth host db
      auth="${rest%%@*}"; rest="${rest#*@}"
      host="${rest%%/*}"; db="${rest#*/}"; db="${db%%\?*}"
      MYSQL_PWD="${auth#*:}" mysql --protocol=TCP -u "${auth%%:*}" -h "${host%%:*}" \
          -P "${host##*:}" -e "DROP DATABASE IF EXISTS \`$db\`; CREATE DATABASE \`$db\`"
      ;;
    *)
      log "unsupported database URL: $URL"
      exit 2
      ;;
  esac
}

cd "$SAMPLE"
reset_schema
log "compiling the server and its tests"
"$MVN" -B -q -Dcodename1.platform=backend -pl shared,backend test-compile "$@"

classes=()
while IFS= read -r file; do
  # A class that only carries shared fields and tests for subclasses is run
  # through them.
  if grep -q "@Test" "$file" && ! grep -qE "^(public )?abstract class" "$file"; then
    rel="${file#backend/src/test/java/}"
    rel="${rel%.java}"
    classes+=("${rel//\//.}")
  fi
done < <(find backend/src/test/java -name '*Test.java' | sort)

if [ "${#classes[@]}" -eq 0 ]; then
  log "no test classes found under $SAMPLE/backend/src/test/java"
  exit 1
fi

failed=()
for class in "${classes[@]}"; do
  reset_schema
  log "$class"
  if ! CN1_DATASOURCE_URL="$URL" "$MVN" -B -q -Dcodename1.platform=backend -pl shared,backend \
      test -Dtest="$class" -Dsurefire.failIfNoSpecifiedTests=false "$@"; then
    failed+=("$class")
  fi
done

log "${#classes[@]} test classes run, ${#failed[@]} failed"
if [ "${#failed[@]}" -ne 0 ]; then
  printf '[database-tests] failed: %s\n' "${failed[@]}" >&2
  exit 1
fi
