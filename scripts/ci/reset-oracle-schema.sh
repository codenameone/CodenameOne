#!/usr/bin/env bash
# Empties an Oracle schema for scripts/ci/sample-server-tests-on-database.sh
# (CN1_TEST_DATABASE_RESET): every view, table and sequence the user owns.
# The identity sequences go with their tables.
#
# usage: reset-oracle-schema.sh <container> <user/password@service>
# The container runs the server and has sqlplus (gvenzl/oracle-free does).
set -euo pipefail
container="${1:?container}"
login="${2:?user/password@service}"
docker exec -i "$container" sqlplus -s -L "$login" <<'SQL'
WHENEVER SQLERROR EXIT FAILURE
BEGIN
  FOR v IN (SELECT view_name FROM user_views) LOOP
    EXECUTE IMMEDIATE 'DROP VIEW "' || v.view_name || '"';
  END LOOP;
  FOR t IN (SELECT table_name FROM user_tables WHERE dropped = 'NO') LOOP
    EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS PURGE';
  END LOOP;
  FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name NOT LIKE 'ISEQ$$%') LOOP
    EXECUTE IMMEDIATE 'DROP SEQUENCE "' || s.sequence_name || '"';
  END LOOP;
  -- Whatever else a migration may create: code, types and synonyms.
  FOR o IN (SELECT object_name, object_type FROM user_objects
            WHERE object_type IN ('PROCEDURE','FUNCTION','PACKAGE','TYPE','SYNONYM','TRIGGER')) LOOP
    EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' "' || o.object_name || '"'
        || CASE WHEN o.object_type = 'TYPE' THEN ' FORCE' ELSE '' END;
  END LOOP;
END;
/
SQL
