#!/bin/bash
# Runs a backend demo on a plain JVM, for the fast local edit-run loop.
#
#   run-javase.sh <fullyQualifiedMainClass> [program args...]
#
# The SAME shared runtime (src/) that the native build translates is compiled here;
# only impl/ differs -- impl/javase instead of impl/parparvm. That is the whole
# point of the split: protocol behaviour cannot drift between the loop you develop
# in and the binary you ship, because there is one copy of it.
#
# What the local runtime deliberately does NOT do: terminate TLS, and therefore
# serve HTTP/2 (Tls and Http2 say so and refuse). Run build.sh for those.
#
# Environment knobs:
#   CN1_BACKEND_DEMO       demo source dir (default demo/petserver)
#   CN1_BACKEND_JDBC_JARS  extra classpath entries for JDBC drivers
#   CN1_BACKEND_JAVA       the java/javac home to use (default: JAVA17_HOME, then PATH)
set -e
cd "$(dirname "$0")"
MAIN="${1:?usage: run-javase.sh <fullyQualifiedMainClass> [args...]}"; shift

JAVA_HOME_DIR="${CN1_BACKEND_JAVA:-${JAVA17_HOME:-}}"
if [ -n "$JAVA_HOME_DIR" ] && [ -x "$JAVA_HOME_DIR/bin/javac" ]; then
    JAVAC="$JAVA_HOME_DIR/bin/javac"; JAVA="$JAVA_HOME_DIR/bin/java"
else
    JAVAC="$(command -v javac)"; JAVA="$(command -v java)"
fi
[ -x "$JAVAC" ] || { echo "no javac found; set CN1_BACKEND_JAVA or JAVA17_HOME"; exit 1; }

DEMO="${CN1_BACKEND_DEMO:-demo/petserver}"
[ -d "$DEMO" ] || { echo "demo directory not found: $DEMO"; exit 1; }
COMMON=""
if [ -d demo/common ]; then COMMON="demo/common"; fi
# gen/ holds COMPILED classes from generate-contract.sh (the server half of the
# shared @RestClient contract), so it goes on the classpath rather than the source
# list -- exactly as build.sh treats it.
if [ -d contract ]; then ./generate-contract.sh --if-needed; fi
GEN=""
if [ -d gen ]; then GEN="gen"; fi

# JDBC drivers are optional: without one, Db.open fails with a message that says
# so, and everything that does not touch a database still runs. sqlite-jdbc needs
# slf4j-api on the classpath as well -- without it the driver's service entry
# throws while being instantiated and DriverManager reports "no suitable driver",
# which names neither the real cause nor the missing jar.
newest_jar() {
    ls -1 "$HOME/.m2/repository/$1/$2/"*/"$2"-*.jar 2>/dev/null \
        | grep -v -- '-sources\.jar$' | grep -v -- '-javadoc\.jar$' \
        | sort -V | tail -1
}
DRIVERS="$CN1_BACKEND_JDBC_JARS"
if [ -z "$DRIVERS" ]; then
    for jar in $(newest_jar org/xerial sqlite-jdbc) $(newest_jar org/slf4j slf4j-api); do
        if [ -z "$DRIVERS" ]; then DRIVERS="$jar"; else DRIVERS="$DRIVERS:$jar"; fi
    done
fi

# A private output directory per run, removed when the JVM exits.
#
# It used to be one shared target/javase-classes that every run deleted and
# rebuilt, which is fine until two runs overlap -- the test suite forks several,
# and the loser's javac fails with "directory not found" on a directory the
# winner removed out from under it. A build this cheap is not worth sharing.
mkdir -p target
OUT="$(mktemp -d "$(pwd)/target/javase.XXXXXX")"
trap 'rm -rf "$OUT"' EXIT
BUILD_CP="$OUT"
if [ -n "$GEN" ]; then BUILD_CP="$BUILD_CP:$GEN"; fi
if [ -n "$DRIVERS" ]; then BUILD_CP="$BUILD_CP:$DRIVERS"; fi
"$JAVAC" -nowarn -encoding UTF-8 -cp "$BUILD_CP" -d "$OUT" \
    $(find src impl/javase $COMMON "$DEMO" -name '*.java')
if [ -n "$GEN" ]; then cp -r "$GEN/." "$OUT/"; fi

CP="$OUT"
if [ -n "$DRIVERS" ]; then CP="$CP:$DRIVERS"; fi
# Not exec: the trap above has to run so the class directory does not accumulate.
# The JVM is in this shell's process group, so Ctrl-C still reaches it.
"$JAVA" -cp "$CP" "$MAIN" "$@"
