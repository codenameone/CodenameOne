#!/bin/bash
# Translates the server-side backend runtime plus one handler class into a native
# binary through ParparVM's clean target.
#
#   build.sh <MainSimpleClassName> <mainPackage> <outBinary> [extra clang flags...]
#
# With CN1_BACKEND_SRC_OUT=<dir> the generated C is left in that directory and the
# host compile is skipped -- which is how package.sh translates once and then
# links the same sources for every target platform. The C the translator emits is
# architecture independent; only the compile differs per target.
#
# The Java is compiled with -bootclasspath pointing at vm/JavaAPI ONLY, so the
# compiler enforces the server-safe surface: a reference to anything outside it
# fails here, in the IDE and in the build, rather than at link time or on a device.
#
# Environment knobs:
#   CN1_BACKEND_DEMO             demo source dir (default demo/petstore)
#   CN1_BACKEND_CFLAGS           extra clang flags
#   CN1_BACKEND_TRANSLATOR_OPTS  extra -D properties for the translator JVM
set -e
cd "$(dirname "$0")"
MAIN="$1"; shift
PKG="$1"; shift
OUTBIN="$1"; shift
EXTRA="$@"

REPO="$(cd ../.. && pwd)"
CC="${CN1_BACKEND_CC:-clang}"

# Virtual threads are a BACKEND feature and their context switch is assembly, so
# the runtime is compiled only here. Set on CN1_BACKEND_CFLAGS rather than at the
# compile line because these flags also travel to the container link through
# cn1-cflags.txt -- one source of truth for the host build and the packaged one.
#
# Device targets leave it unset, which is the point: Xcode does not recognise a
# .S (it files one under `lastKnownFileType = file` into the RESOURCES phase, so
# it is never assembled), and the iOS link failed on "_cn1VirtualThreadSwitch,
# referenced from _cn1VirtualThreadYield". Gated off there is no reference to
# resolve. See cn1_virtual_thread.h.
CN1_BACKEND_CFLAGS="${CN1_BACKEND_CFLAGS:-} -DCN1_VIRTUAL_THREADS=1"
J8="${JDK_8_HOME:?set JDK_8_HOME to a JDK 8 home}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1backend.XXXXXX")"

TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
if [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator -am package -DskipTests)
fi
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/bench-asm-classpath.txt"
# The cached file holds ABSOLUTE paths into whichever maven repo generated it, so a
# copy from another machine -- or one written before the repo moved -- points at
# jars that are not there. Reusing it blindly fails much later as
# "NoClassDefFoundError: org/objectweb/asm/ClassVisitor", which names neither the
# cache nor the missing jar. Check every entry and regenerate when one is gone.
asm_cp_valid() {
    [ -f "$ASM_CP_FILE" ] || return 1
    cp_value="$(cat "$ASM_CP_FILE")"
    [ -n "$cp_value" ] || return 1
    old_ifs="$IFS"; IFS=:
    for entry in $cp_value; do
        if [ ! -e "$entry" ]; then IFS="$old_ifs"; return 1; fi
    done
    IFS="$old_ifs"
    return 0
}
if ! asm_cp_valid; then
    command -v mvn >/dev/null 2>&1 || {
        echo "the ASM classpath cache is missing or stale and maven is not on PATH;"
        echo "install maven, or regenerate $ASM_CP_FILE on this machine"
        exit 1
    }
    rm -f "$ASM_CP_FILE"
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator dependency:build-classpath \
        -Dmdep.outputFile=target/bench-asm-classpath.txt)
fi
ASM_CP="$(cat "$ASM_CP_FILE")"

# the C runtime resources the translator emits from its classpath
# cn1_sqlite3.c is synced too: it carries our build options for the bundled
# engine, and a stale copy in target/classes silently builds the old ones.
for f in cn1_globals.h cn1_globals.m nativeMethods.m cn1_intrinsics.h cn1_sqlite3.c \
         cn1_virtual_thread.h cn1_virtual_thread.c cn1_virtual_thread_asm.S; do
    cp "$REPO/vm/ByteCodeTranslator/src/$f" "$TRANSLATOR/$f"
done

# Compiled once and shared by every build in this tree, so concurrent builds --
# the test suite forks several -- must not race to write it. It is built into a
# private directory and moved into place, which is atomic; a loser of the race
# throws its copy away rather than merging into the winner's.
JAVAAPI="$REPO/vm/backend/target/javaapi-classes"
# Rebuild when a JavaAPI SOURCE is newer than what was compiled, not merely when
# nothing is there. Existence alone was the test, so editing vm/JavaAPI and
# rebuilding silently kept the old classes -- a LinkedHashMap change measured as
# having no effect for exactly this reason, and nothing in the output said the
# edit had been ignored.
if [ -f "$JAVAAPI/java/lang/Object.class" ] \
   && [ -n "$(find "$REPO/vm/JavaAPI/src" -name '*.java' \
              -newer "$JAVAAPI/java/lang/Object.class" -print -quit)" ]; then
    echo "JavaAPI sources changed; recompiling $JAVAAPI"
    rm -rf "$JAVAAPI"
fi
if [ ! -f "$JAVAAPI/java/lang/Object.class" ]; then
    STAGING="$(mktemp -d "$REPO/vm/backend/target/javaapi.XXXXXX")"
    "$J8/bin/javac" -nowarn -source 1.8 -target 1.8 -d "$STAGING" \
        $(find "$REPO/vm/JavaAPI/src" -name '*.java')
    if [ ! -f "$JAVAAPI/java/lang/Object.class" ] && mv "$STAGING" "$JAVAAPI" 2>/dev/null; then
        :
    else
        rm -rf "$STAGING"
    fi
fi

mkdir -p "$WORK/classes"
# gen/ holds the classes RestServerAnnotationProcessor produced from the shared
# @RestClient contract (see generate-contract.sh). It goes on -classpath, never on
# -bootclasspath: the bootclasspath IS the server-safe surface, and generated code
# is application code like any other.
# The petserver demo is built FROM the shared contract, so the generated half has
# to exist before javac runs. Doing it here rather than expecting the developer
# (or CI) to remember is what keeps a fresh checkout buildable in one command.
if [ -d contract ]; then ./generate-contract.sh --if-needed; fi
GEN=""
if [ -d gen ]; then GEN="gen"; fi
# src/ is the shared runtime -- protocol logic, pure Java, identical on every
# target. impl/parparvm holds the classes backed by natives; impl/javase holds
# their Java SE twins and is what the local dev loop compiles instead.
#
# One demo per directory: the translator refuses a classpath with two main classes
# on it, so the demo tree cannot be compiled wholesale.
DEMO="${CN1_BACKEND_DEMO:-demo/petstore}"
[ -d "$DEMO" ] || { echo "demo directory not found: $DEMO"; exit 1; }
# demo/common holds what every front end shares (the service implementation); each
# demo directory holds exactly one main class.
COMMON=""
[ -d demo/common ] && COMMON="demo/common"
"$J8/bin/javac" -nowarn -encoding UTF-8 -bootclasspath "$JAVAAPI" ${GEN:+-cp "$GEN"} -source 1.8 -target 1.8 \
    -d "$WORK/classes" $(find src impl/parparvm $COMMON "$DEMO" -name '*.java')
if [ -n "$GEN" ]; then cp -r "$GEN/." "$WORK/classes/"; fi

mkdir -p "$WORK/out"
# The native sources have to be in the source root BEFORE the translator runs, not
# after. Two things read that directory up front: readNativeFiles, which keeps a
# Java method alive when only native code calls it, and NativeSignatureVerifier,
# which checks every declared native against an actual implementation. Copying
# afterwards -- which is what this did -- left the verifier reporting every backend
# native as unimplemented, so the one gate that catches a mistyped symbol was blind
# to this whole module.
SRCDIR="$WORK/out/dist/$MAIN-src"
mkdir -p "$SRCDIR"
cp native/*.c "$SRCDIR/"
# Every native here is ours, so a name that does not match the one the translator
# generates is always a bug -- never a symbol living in some prebuilt library we
# do not control. That is worth forcing on, because the failure is silent in both
# directions: the dead-code pass keeps a Java native alive only when its C symbol
# appears (BytecodeMethod.isMethodUsedByNative), so a misspelled name both leaves
# the C function uncalled AND drops the Java method, and the build stays green
# with the feature inert. VirtualThread.isVirtual() shipped that way -- the body
# was written isVirtualImpl__R_boolean where the signature rule gives
# isVirtualImpl___R_boolean, since the empty argument list contributes its own
# leading underscore before _R. Nothing called it, so nothing failed to link.
: "${CN1_NATIVE_VERIFY:=strict}"
export CN1_NATIVE_VERIFY
if [ "${CN1_BACKEND_HTTPS:-1}" = "0" ]; then
    # Dropping these files drops their natives, and a native with no C symbol
    # takes its JAVA method with it (BytecodeMethod.isMethodUsedByNative) -- so a
    # program that still calls Crypto or Web links fine and does nothing. The
    # verifier above is the only thing that notices, which is part of why it is
    # on for every build rather than just this one. This mode is for measuring
    # what TLS costs in binary size, on a program that does not use it.
    rm -f "$SRCDIR/cn1_backend_web.c" "$SRCDIR/cn1_backend_crypto.c" \
          "$SRCDIR/cn1_backend_tls.c" "$SRCDIR/cn1_backend_http2.c"
    # cn1_backend_tlsclient.c is NOT removed, it is stubbed. Tcp always declares
    # its natives, and a native whose symbol is missing is dropped from the Java
    # side by the dead-code pass -- startTls would then silently do nothing.
    CN1_BACKEND_CFLAGS="$CN1_BACKEND_CFLAGS -DCN1_BACKEND_NO_TLS"
fi
# The bundled SQLite engine is emitted only when the translator is told to; the
# backend needs it for com.codename1.backend.Db. Set CN1_BACKEND_SQLITE=0 to leave
# it out and see what persistence costs in binary size.
SQLITE_OPT="-Dcn1.sqlite=true"
if [ "${CN1_BACKEND_SQLITE:-1}" = "0" ]; then
    SQLITE_OPT=""
    # cn1_backend_db.c stays in the build and compiles to stubs. Leaving it out
    # would drop the Db natives, and a native with no C symbol takes its Java
    # method with it, so Db.open would link and silently do nothing; the stubs
    # answer "could not open" instead, which Db already turns into an IOException.
    CN1_BACKEND_CFLAGS="$CN1_BACKEND_CFLAGS -DCN1_BACKEND_NO_SQLITE"
fi
# Checked casts are OFF by default in ParparVM and ON here, which is the one place
# the server target deliberately departs from the mobile one.
#
# On a phone an unchecked CHECKCAST costs one user a crash. On a server the object
# whose type is wrong arrived from the network, so a failed cast is not a bug the
# developer will hit in testing -- it is an input a client chose, and reading the
# wrong type's fields out of it kills the process and every connection it was
# serving. A catchable ClassCastException is worth its few percent here.
#
# The generated dispatchers do not RELY on this (they narrow with instanceof, see
# RestServerAnnotationProcessor); it is the backstop for handler code that does.
# CN1_BACKEND_CHECKED_CASTS=0 turns it off to measure what it costs.
CAST_OPT="-Dcn1.checkedCasts=true"
if [ "${CN1_BACKEND_CHECKED_CASTS:-1}" = "0" ]; then CAST_OPT=""; fi
"$J8/bin/java" $SQLITE_OPT $CAST_OPT $CN1_BACKEND_TRANSLATOR_OPTS -cp "$TRANSLATOR:$ASM_CP" \
    com.codename1.tools.translator.ByteCodeTranslator \
    clean "$JAVAAPI;$WORK/classes" "$WORK/out" "$MAIN" "$PKG" "$MAIN" 1.0 clean none \
    > "$WORK/translate.log" 2>&1 || { echo "TRANSLATE FAILED"; tail -30 "$WORK/translate.log"; exit 1; }

# Outbound TLS is libcurl's job (see cn1_backend_web.c) and the auth primitives are
# OpenSSL's (see cn1_backend_crypto.c). Set CN1_BACKEND_HTTPS=0 to drop both and
# their dependencies; a fully static build needs static versions, which is why this
# is a switch rather than an assumption.
CURL_LIB="-lcurl -lssl -lcrypto -lnghttp2"
SSL_FLAGS=""
if [ "${CN1_BACKEND_HTTPS:-1}" = "0" ]; then
    CURL_LIB=""
else
    # macOS ships libcrypto but not its headers; Homebrew's OpenSSL is the usual
    # source. On Linux the distro's -dev package puts them where clang looks.
    for prefix in "$OPENSSL_PREFIX" /opt/homebrew/opt/openssl@3 /usr/local/opt/openssl@3; do
        if [ -n "$prefix" ] && [ -f "$prefix/include/openssl/sha.h" ]; then
            SSL_FLAGS="-I$prefix/include -L$prefix/lib"
            break
        fi
    done
    # nghttp2 provides the HTTP/2 framing (see cn1_backend_http2.c).
    for prefix in "$NGHTTP2_PREFIX" /opt/homebrew/opt/libnghttp2 /opt/homebrew/opt/nghttp2 /usr/local/opt/libnghttp2; do
        if [ -n "$prefix" ] && [ -f "$prefix/include/nghttp2/nghttp2.h" ]; then
            SSL_FLAGS="$SSL_FLAGS -I$prefix/include -L$prefix/lib"
            break
        fi
    done
fi
if [ -n "$CN1_BACKEND_SRC_OUT" ]; then
    rm -rf "$CN1_BACKEND_SRC_OUT"
    mkdir -p "$(dirname "$CN1_BACKEND_SRC_OUT")"
    cp -R "$SRCDIR" "$CN1_BACKEND_SRC_OUT"
    # The flags derived above -- the -D that turns SQLite or TLS into stubs --
    # travel WITH the sources. package.sh links these in a container and cannot
    # see this shell's variables, so without this the switches produced a source
    # tree the link could not compile ("cn1_sqlite3.h file not found") while the
    # host build worked.
    echo "$CN1_BACKEND_CFLAGS" > "$CN1_BACKEND_SRC_OUT/cn1-cflags.txt"
    echo "translated $MAIN into $CN1_BACKEND_SRC_OUT"
    exit 0
fi

# -fwrapv -fno-strict-aliasing -fno-builtin-fmod(f) are MANDATORY for generated C
# (Java wrapping arithmetic; clang -O3 provably miscompiles without them).
$CC -O3 -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
    $CN1_BACKEND_CFLAGS $EXTRA $SSL_FLAGS -I"$SRCDIR" "$SRCDIR"/*.c "$SRCDIR"/*.S \
    -lm -lpthread $CURL_LIB -o "$OUTBIN" \
    2> "$WORK/cc.log" || { echo "COMPILE FAILED"; tail -40 "$WORK/cc.log"; exit 1; }
echo "built $OUTBIN (workdir $WORK)"
