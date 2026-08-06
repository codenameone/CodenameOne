#!/usr/bin/env bash
#
# Compiles the SQLCipher-backed database package the way an application build does.
#
# That package is deliberately excluded from the Android port jar: it references net.zetetic,
# which is only on the classpath of applications that actually encrypt a database, and the builder
# deletes it for everyone else. The consequence is that no ordinary build of this repository ever
# compiles it, so a member of com.codename1.impl.android that it reaches has to be public and
# nothing local notices when it is not. That has now been the failure twice, each time found by a
# device job forty minutes into CI.
#
# This compiles the package against the rest of the port with stub net.zetetic and android classes,
# which is enough to catch cross-package visibility, signature drift and ordinary syntax errors in
# seconds.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

fail() {
    echo "check-android-cipher-package: $*" >&2
    exit 1
}

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
command -v "$JAVAC" >/dev/null 2>&1 || JAVAC=javac

ANDROID_JAR="$(find "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}/platforms" \
    -name 'android.jar' 2>/dev/null | sort | tail -1 || true)"
[ -n "$ANDROID_JAR" ] || fail "no android.jar found; set ANDROID_HOME"

CIPHER_DIR="$REPO_ROOT/Ports/Android/src/com/codename1/impl/android/cipher"
[ -d "$CIPHER_DIR" ] || fail "the cipher package is missing from $CIPHER_DIR"

# Minimal stubs for the SQLCipher API the package uses. Only the shapes it references: this is a
# visibility and signature check, not a functional one.
STUBS="$WORK_DIR/stubs/net/zetetic/database/sqlcipher"
mkdir -p "$STUBS"
cat > "$STUBS/SQLiteDatabase.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
import android.database.Cursor;
public class SQLiteDatabase {
    public interface CursorFactory {
        Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable,
                SQLiteQuery query);
    }
    public static SQLiteDatabase openOrCreateDatabase(java.io.File f, String password,
            CursorFactory factory, Object hook) { return null; }
    public void beginTransaction() { }
    public void setTransactionSuccessful() { }
    public void endTransaction() { }
    public void execSQL(String sql) { }
    public void close() { }
    public String getPath() { return null; }
    public SQLiteStatement compileStatement(String sql) { return null; }
    public Cursor rawQuery(String sql, String[] args) { return null; }
    public Cursor rawQueryWithFactory(CursorFactory factory, String sql, String[] args,
            String editTable) { return null; }
}
JAVA
cat > "$STUBS/SQLiteProgram.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
public class SQLiteProgram {
    public void bindNull(int index) { }
    public void bindString(int index, String value) { }
    public void bindBlob(int index, byte[] value) { }
    public void bindLong(int index, long value) { }
    public void bindDouble(int index, double value) { }
}
JAVA
cat > "$STUBS/SQLiteStatement.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
public class SQLiteStatement extends SQLiteProgram {
    public void execute() { }
    public void close() { }
}
JAVA
cat > "$STUBS/SQLiteQuery.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
public class SQLiteQuery extends SQLiteProgram { }
JAVA
cat > "$STUBS/SQLiteCursorDriver.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
public interface SQLiteCursorDriver { }
JAVA
cat > "$STUBS/SQLiteCursor.java" <<'JAVA'
package net.zetetic.database.sqlcipher;
// AbstractCursor rather than the Cursor interface, so the stub does not have to restate the whole
// interface just to satisfy javac.
public class SQLiteCursor extends android.database.AbstractCursor {
    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) { }
    public int getCount() { return 0; }
    public String[] getColumnNames() { return null; }
    public String getString(int column) { return null; }
    public short getShort(int column) { return 0; }
    public int getInt(int column) { return 0; }
    public long getLong(int column) { return 0; }
    public float getFloat(int column) { return 0; }
    public double getDouble(int column) { return 0; }
    public boolean isNull(int column) { return false; }
}
JAVA

# Compiled port classes, not sources. The port needs a support-library classpath this check has
# no business assembling, and compiled classes are also the more faithful target: they carry the
# exact member visibility the application build links against, which is the thing that keeps
# breaking.
# The framework classes the port and the cipher package are written against.
CORE_JAR="$(find "${CN1_LOCAL_REPO:-/tmp/cn1-local-repo}" "$HOME/.m2/repository" \
    -path '*com/codenameone/codenameone-core/*' -name 'codenameone-core-*.jar' \
    ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | sort | tail -1 || true)"
[ -n "$CORE_JAR" ] || fail "no codenameone-core jar found; build the core first"

PORT_CLASSES="$REPO_ROOT/maven/android/target/classes"
[ -d "$PORT_CLASSES" ] || fail "no compiled Android port at $PORT_CLASSES; run
  mvn -f maven/pom.xml -Pcompile-android -pl android -am -DskipTests compile"
[ -f "$PORT_CLASSES/com/codename1/impl/android/AndroidImplementation.class" ] \
    || fail "$PORT_CLASSES has no AndroidImplementation; rebuild the Android port"

# The build copies the cipher package's .java files into the output as resources, for the builder
# to stage into the generated application - that is expected. What must not be there is compiled
# output: the module excludes the package from compilation, which is the whole reason nothing here
# type-checks it and this script exists.
CIPHER_CLASSES="$PORT_CLASSES/com/codename1/impl/android/cipher"
if [ -d "$CIPHER_CLASSES" ] && [ -n "$(find "$CIPHER_CLASSES" -name '*.class' 2>/dev/null)" ]; then
    fail "the cipher package is being compiled into the port output. It is meant to be excluded
  so the builder can delete it for applications that never encrypt; if that changed, this check
  and the deletable-package arrangement both need revisiting."
fi

OUT="$WORK_DIR/classes"
mkdir -p "$OUT"

set +e
"$JAVAC" -nowarn -proc:none -d "$OUT" \
    -source 8 -target 8 \
    -bootclasspath "$ANDROID_JAR" \
    -cp "$ANDROID_JAR:$PORT_CLASSES:$CORE_JAR" \
    -sourcepath "$WORK_DIR/stubs" \
    "$WORK_DIR/stubs/net/zetetic/database/sqlcipher"/*.java \
    "$CIPHER_DIR"/*.java 2>"$WORK_DIR/javac.log"
STATUS=$?
set -e

if [ $STATUS -ne 0 ]; then
    echo "check-android-cipher-package: the SQLCipher-backed package does not compile." >&2
    echo "This package is excluded from the port jar, so nothing else in a local build compiles" >&2
    echo "it -- a device job would be the next thing to notice." >&2
    echo >&2
    grep -E "error:" "$WORK_DIR/javac.log" | head -20 >&2
    exit 1
fi

echo "check-android-cipher-package: the SQLCipher-backed package compiles against the port."
