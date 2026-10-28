#!/usr/bin/env bash
#
# Runs the SQLAlchemy conformance programs against real database servers and
# requires the output SQLite (and CPython) give.
#
# The conformance suite runs them on SQLite only, so the PostgreSQL, MySQL and
# MariaDB dialects -- which go through the backend's own drivers via _cn1db --
# are exercised here. Each program is transpiled as a backend build (the
# backend jar on the class path generates the database bridge) and run on the
# JVM with the server URL as its argument (the programs read sys.argv).
#
#   CN1_SQLA_POSTGRES=postgres://u:p@127.0.0.1:5432/db?sslmode=disable
#   CN1_SQLA_MYSQL=mysql://u:p@127.0.0.1:3306/db?sslmode=disable
#   CN1_SQLA_MARIADB=mariadb://...
#   JAVA17_HOME=<jdk 17+>
#   scripts/python-backend/sqlalchemy-realdb-test.sh [maven repo]
#
# An engine whose variable is unset is skipped with a note; at least one must
# be set. Needs codenameone-core, -python-runtime, -python-transpiler and
# -backend installed in the maven repo (default: <repo>/.m2-repo).
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License v2 with Classpath Exception.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
M2="${1:-$REPO/.m2-repo}"
J17="${JAVA17_HOME:?set JAVA17_HOME}"
CONF="${CN1_SQLA_CONF:-$REPO/maven/python-transpiler/src/test/resources/conformance}"
PROGRAMS="${CN1_SQLA_PROGRAMS:-sqlalchemy_core sqlalchemy_orm sqlalchemy_relations sqlalchemy_query}"
art() { echo "$M2/$1/$2/$3/$2-$3.jar"; }
CORE=$(art com/codenameone codenameone-core 8.0-SNAPSHOT)
RUNTIME=$(art com/codenameone codenameone-python-runtime 8.0-SNAPSHOT)
TRANSPILER=$(art com/codenameone codenameone-python-transpiler 8.0-SNAPSHOT)
BACKEND=$(art com/codenameone codenameone-backend 8.0-SNAPSHOT)
JAVAAPI=$(art com/codename1/parparvm JavaAPI 1.0-SNAPSHOT)
for j in "$CORE" "$RUNTIME" "$TRANSPILER" "$BACKEND" "$JAVAAPI"; do
    [ -f "$j" ] || { echo "missing $j -- install it into $M2" >&2; exit 1; }
done

ENGINES=""
[ -n "${CN1_SQLA_POSTGRES:-}" ] && ENGINES="$ENGINES postgresql"
[ -n "${CN1_SQLA_MYSQL:-}" ] && ENGINES="$ENGINES mysql"
[ -n "${CN1_SQLA_MARIADB:-}" ] && ENGINES="$ENGINES mariadb"
[ -n "$ENGINES" ] || { echo "set CN1_SQLA_POSTGRES, CN1_SQLA_MYSQL and/or CN1_SQLA_MARIADB" >&2; exit 2; }

WORK="${CN1_SQLA_WORK:-$(mktemp -d "${TMPDIR:-/tmp}/sqla-realdb.XXXXXX")}"
mkdir -p "$WORK"
failed=0
for prog in $PROGRAMS; do
    APP="$WORK/$prog"
    mkdir -p "$APP/src" "$APP/gen" "$APP/classes"
    cp "$CONF/$prog.py" "$APP/src/main.py"
    "$J17/bin/java" -cp "$TRANSPILER" com.codename1.python.transpiler.api.Main \
        -cp "$JAVAAPI:$CORE:$BACKEND" "$APP/src" "$APP/gen" main >"$APP/transpile.log" 2>&1 \
        || { cat "$APP/transpile.log" >&2; exit 1; }
    [ -f "$APP/gen/com/codename1/generated/python/BackendDb.java" ] \
        || { echo "$prog: no database bridge was generated" >&2; exit 1; }
    mkdir -p "$APP/gen/com/pybackend"
    cat >"$APP/gen/com/pybackend/Main.java" <<'JAVA'
package com.pybackend;

public class Main {
    public static void main(String[] args) {
        com.codename1.python.runtime.Py.setArgv("main", args);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
JAVA
    # Each program starts from an empty database: whatever an earlier program
    # (or an earlier run) left behind -- tables, sequences, types -- would
    # otherwise leak into its output.
    cat >"$APP/gen/com/pybackend/Reset.java" <<'JAVA'
package com.pybackend;

import com.codename1.backend.Database;

public class Reset {
    public static void main(String[] args) throws Exception {
        Database db = Database.open(args[0]);
        try {
            if ("postgresql".equals(db.dialect().getName())) {
                db.execute("DROP SCHEMA public CASCADE", new Object[0]);
                db.execute("CREATE SCHEMA public", new Object[0]);
            }
        } finally {
            db.close();
        }
    }
}
JAVA
    (cd "$APP/classes" && unzip -qo "$RUNTIME" 'com/*' && unzip -qo "$CORE" 'com/codename1/util/*')
    "$J17/bin/javac" -nowarn --release 17 -d "$APP/classes" $(find "$REPO/scripts/python-bench/hostfs" -name '*.java')
    "$J17/bin/javac" -nowarn --release 17 -cp "$APP/classes:$BACKEND" -d "$APP/classes" \
        $(find "$APP/gen" -name '*.java')
    for engine in $ENGINES; do
        case "$engine" in
            postgresql) url="$CN1_SQLA_POSTGRES" ;;
            mysql) url="$CN1_SQLA_MYSQL" ;;
            mariadb) url="$CN1_SQLA_MARIADB" ;;
        esac
        out="$APP/$engine.out"
        "$J17/bin/java" -cp "$APP/classes:$BACKEND:$CORE" com.pybackend.Reset "$url"
        "$J17/bin/java" -cp "$APP/classes:$BACKEND:$CORE" com.pybackend.Main "$url" >"$out" 2>&1 || true
        if diff -u "$CONF/$prog.out" "$out" >"$APP/$engine.diff"; then
            echo "PASS $prog on $engine"
        else
            echo "FAIL $prog on $engine (diff: $APP/$engine.diff)"
            head -30 "$APP/$engine.diff"
            failed=1
        fi
    done
done
exit $failed
