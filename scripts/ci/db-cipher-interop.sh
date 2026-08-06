#!/usr/bin/env bash
#
# Proves that an encrypted database really is portable between platforms.
#
# The claim this project makes is that every port reads and writes one on-disk format, so a
# database written on a phone can be opened in the simulator. That claim is worth nothing untested:
# a cipher misconfiguration produces files that each platform can read perfectly well on its own and
# nothing else can touch, and no single-platform test would notice.
#
# The oracle is the stock sqlcipher command line client, which is neither of our implementations.
# If our file and its file are mutually readable, the format is right.
#
# A fixed raw key is used rather than a passphrase, deliberately: it takes the key derivation out of
# the comparison, so a failure points at the cipher, page size or HMAC configuration rather than at
# PBKDF2 parameters. A second leg then repeats the check with a passphrase to cover the derivation.
#
# Usage: scripts/ci/db-cipher-interop.sh [work-dir]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK_DIR="${1:-$(mktemp -d)}"
mkdir -p "$WORK_DIR"

# 32 bytes, written the way every engine accepts a raw key.
RAW_KEY="x'000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f'"
PASSPHRASE="correct horse battery staple"

fail() {
    echo "db-cipher-interop: $1" >&2
    exit 1
}

command -v sqlcipher >/dev/null 2>&1 || fail "the sqlcipher client is required (brew install sqlcipher)"

# The local repository is not always ~/.m2: settings.xml can redirect it, and CI does.
LOCAL_REPO="${CN1_LOCAL_REPO:-}"
if [ -z "$LOCAL_REPO" ]; then
    LOCAL_REPO="$(mvn -q -f "$REPO_ROOT/maven/pom.xml" help:evaluate \
        -Dexpression=settings.localRepository -DforceStdout 2>/dev/null | tail -1 || true)"
fi
[ -d "$LOCAL_REPO" ] || LOCAL_REPO="$HOME/.m2/repository"

JDBC_JAR="$(find "$LOCAL_REPO/io/github/willena/sqlite-jdbc" -name 'sqlite-jdbc-*.jar' \
    ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | sort | tail -1 || true)"
[ -n "$JDBC_JAR" ] || fail "the SQLite JDBC driver is not under $LOCAL_REPO; build maven/sqlite-jdbc first"

echo "db-cipher-interop: using $JDBC_JAR"

cat > "$WORK_DIR/Interop.java" <<'JAVA'
import java.sql.*;
import java.util.Properties;
import org.sqlite.mc.SQLiteMCSqlCipherConfig;

/** Writes and reads through the driver the simulator uses. */
public class Interop {
    static Properties props(String key) throws Exception {
        // getV4Defaults, not getDefault: the latter selects the driver's own cipher variant, which
        // real SQLCipher cannot read. This is the single most important line in the whole setup.
        return SQLiteMCSqlCipherConfig.getV4Defaults().withKey(key).build().toProperties();
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0], file = args[1], key = args[2];
        Class.forName("org.sqlite.JDBC");
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file, props(key));
        Statement s = c.createStatement();
        if ("write".equals(mode)) {
            s.executeUpdate("CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT)");
            s.executeUpdate("INSERT INTO t VALUES (1, 'written-by-the-driver')");
            s.executeUpdate("PRAGMA user_version = 11");
        } else if ("append".equals(mode)) {
            s.executeUpdate("INSERT INTO t VALUES (2, 'appended-by-the-driver')");
        } else {
            ResultSet rs = s.executeQuery("SELECT id, name FROM t ORDER BY id");
            while (rs.next()) {
                System.out.println("row " + rs.getInt(1) + " " + rs.getString(2));
            }
            rs.close();
            ResultSet uv = s.executeQuery("PRAGMA user_version");
            uv.next();
            System.out.println("user_version " + uv.getInt(1));
            uv.close();
        }
        s.close();
        c.close();
    }
}
JAVA

javac -cp "$JDBC_JAR" -d "$WORK_DIR" "$WORK_DIR/Interop.java"
CP="$JDBC_JAR:$WORK_DIR"

run_leg() {
    local label="$1" key="$2" pragma_key="$3"
    local a="$WORK_DIR/${label}-driver.db" b="$WORK_DIR/${label}-client.db"
    rm -f "$a" "$b"

    echo "--- $label: driver writes, sqlcipher client reads ---"
    java -cp "$CP" Interop write "$a" "$key" 2>/dev/null

    head -c 15 "$a" | grep -q "SQLite format 3" \
        && fail "$label: the file is plaintext; nothing was encrypted"

    local out
    out="$(sqlcipher "$a" "PRAGMA key=$pragma_key; SELECT id||'|'||name FROM t; PRAGMA user_version;" 2>&1)"
    echo "$out" | grep -q "written-by-the-driver" \
        || fail "$label: the sqlcipher client could not read the driver's database: $out"
    echo "$out" | grep -q "^11$" \
        || fail "$label: user_version did not survive: $out"

    echo "--- $label: sqlcipher client writes, driver reads ---"
    sqlcipher "$b" "PRAGMA key=$pragma_key; CREATE TABLE t(id INTEGER PRIMARY KEY, name TEXT); \
        INSERT INTO t VALUES(1,'written-by-the-client'); PRAGMA user_version=11;" >/dev/null 2>&1
    out="$(java -cp "$CP" Interop read "$b" "$key" 2>/dev/null)"
    echo "$out" | grep -q "written-by-the-client" \
        || fail "$label: the driver could not read the client's database: $out"

    echo "--- $label: round trip, both directions ---"
    java -cp "$CP" Interop append "$b" "$key" 2>/dev/null
    out="$(sqlcipher "$b" "PRAGMA key=$pragma_key; SELECT count(*) FROM t;" 2>&1)"
    echo "$out" | grep -q "^2$" \
        || fail "$label: the client did not see the driver's appended row: $out"

    echo "$label: OK"
}

# Raw key first: isolates the cipher configuration from the key derivation.
run_leg "rawkey" "$RAW_KEY" "\"$RAW_KEY\""
# Then a passphrase, which additionally exercises the PBKDF2 parameters.
run_leg "passphrase" "$PASSPHRASE" "'$PASSPHRASE'"

echo
echo "db-cipher-interop: encrypted databases are interchangeable with the reference SQLCipher client"
