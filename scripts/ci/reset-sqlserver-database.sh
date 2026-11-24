#!/usr/bin/env bash
# Drops and recreates a SQL Server database for
# scripts/ci/sample-server-tests-on-database.sh (CN1_TEST_DATABASE_RESET).
#
# usage: reset-sqlserver-database.sh <container> <sa password> <database>
# The container runs the server and has sqlcmd (the mssql/server image does).
set -euo pipefail
container="${1:?container}"
password="${2:?sa password}"
database="${3:?database}"
sqlcmd=$(docker exec "$container" sh -c 'ls -d /opt/mssql-tools18/bin/sqlcmd /opt/mssql-tools/bin/sqlcmd 2>/dev/null | head -1')
docker exec "$container" "$sqlcmd" -C -b -S localhost -U sa -P "$password" -Q \
  "IF DB_ID('$database') IS NOT NULL BEGIN ALTER DATABASE [$database] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$database]; END; CREATE DATABASE [$database];"
