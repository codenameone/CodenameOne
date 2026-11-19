#!/usr/bin/env bash
# Starts a SQL Server or Oracle container for
# scripts/ci/sample-server-tests-on-database.sh and writes the variables that
# script reads -- CN1_TEST_DATABASE_URL and CN1_TEST_DATABASE_RESET -- to
# $GITHUB_ENV (or prints them when that is unset).
#
# usage: start-test-database.sh sqlserver|oracle
#
# A step rather than a service container, so a matrix leg pulls only its own
# engine's image. Oracle Free is the image the gvenzl project publishes; 19c
# itself is not pullable without an Oracle account.
set -euo pipefail
engine="${1:?sqlserver or oracle}"
here="$(cd "$(dirname "$0")" && pwd)"
out="${GITHUB_ENV:-/dev/stdout}"

wait_for() {
  local tries="$1"; shift
  for _ in $(seq 1 "$tries"); do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "the $engine container did not come up" >&2
  docker logs --tail 50 "cn1-test-$engine" >&2 || true
  return 1
}

case "$engine" in
  sqlserver)
    password='Cn1-Secret-9x'
    docker run -d --name cn1-test-sqlserver -e ACCEPT_EULA=Y -e "MSSQL_SA_PASSWORD=$password" \
      -p 1433:1433 mcr.microsoft.com/mssql/server:2022-latest >/dev/null
    wait_for 60 docker exec cn1-test-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -b -S localhost \
      -U sa -P "$password" -Q 'SELECT 1'
    "$here/reset-sqlserver-database.sh" cn1-test-sqlserver "$password" cn1
    {
      echo "CN1_TEST_DATABASE_URL=sqlserver://sa:$password@127.0.0.1:1433/cn1?sslmode=disable"
      echo "CN1_TEST_DATABASE_RESET=$here/reset-sqlserver-database.sh cn1-test-sqlserver '$password' cn1"
    } >> "$out"
    ;;
  oracle)
    docker run -d --name cn1-test-oracle -e ORACLE_PASSWORD=cn1secret -e APP_USER=cn1 \
      -e APP_USER_PASSWORD=cn1secret -p 1521:1521 gvenzl/oracle-free:23-slim >/dev/null
    wait_for 120 docker exec cn1-test-oracle healthcheck.sh
    # Migrations serialise on DBMS_LOCK, which an ordinary user may not call.
    docker exec -i cn1-test-oracle sqlplus -s -L sys/cn1secret@FREEPDB1 as sysdba <<'SQL'
WHENEVER SQLERROR EXIT FAILURE
GRANT EXECUTE ON SYS.DBMS_LOCK TO cn1;
SQL
    {
      echo "CN1_TEST_DATABASE_URL=oracle://cn1:cn1secret@127.0.0.1:1521/FREEPDB1?sslmode=disable"
      echo "CN1_TEST_DATABASE_RESET=$here/reset-oracle-schema.sh cn1-test-oracle cn1/cn1secret@FREEPDB1"
    } >> "$out"
    ;;
  *)
    echo "start-test-database.sh: sqlserver or oracle, not $engine" >&2
    exit 2
    ;;
esac
