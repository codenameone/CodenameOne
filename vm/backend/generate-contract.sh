#!/bin/bash
# Generates the server half of the shared @RestClient contract.
#
# This runs the REAL goal a Codename One project runs -- cn1:process-annotations
# at PROCESS_CLASSES, from contract/pom.xml -- rather than a bespoke invocation of
# the processor. If this works, the production path works.
#
# The contract itself is compiled against the CN1 core (it names @GET, OnComplete
# and Response). The GENERATED classes reference nothing outside java.*, which is
# what lets them link into a server binary with no platform layer -- so the
# contract's own class is dropped afterwards and only the generated pair ships.
#
# With --if-needed it returns immediately when gen/ is already up to date, which
# is how build.sh and run-javase.sh can depend on it without paying for maven on
# every build.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
M2="${CN1_M2:-$REPO/.m2-repo}"

IF_NEEDED=0
if [ "$1" = "--if-needed" ]; then IF_NEEDED=1; fi

up_to_date() {
    [ -d gen ] || return 1
    [ -n "$(find gen -name '*.class' 2>/dev/null | head -1)" ] || return 1
    [ -z "$(find contract -name '*.java' -newer gen 2>/dev/null | head -1)" ] || return 1
    # The GENERATOR counts as an input too. Only the contract sources were
    # checked, so editing RestServerAnnotationProcessor -- or just rebuilding the
    # plugin -- left gen/ looking current, and build.sh and run-javase.sh went on
    # exercising the previous dispatcher and codecs. A parity run then reported on
    # a generator change that was not in the program it tested, which is the worst
    # kind of green.
    for artifact in \
        "$REPO/maven/codenameone-maven-plugin/target/classes" \
        "$REPO/maven/codenameone-maven-plugin/target"/codenameone-maven-plugin-*.jar
    do
        [ -e "$artifact" ] || continue
        [ -z "$(find "$artifact" -newer gen 2>/dev/null | head -1)" ] || return 1
    done
    return 0
}

if [ "$IF_NEEDED" = "1" ] && up_to_date; then
    exit 0
fi

# One writer at a time. build.sh and run-javase.sh both call this, and the test
# suite forks several of them at once against this one working tree -- without a
# lock the second fork's javac reads gen/ while the first is between its `rm -rf`
# and its `cp`, and fails on classes that exist in both before and after.
#
# mkdir is the atomic primitive that exists everywhere; flock is not on macOS.
mkdir -p target
LOCK="$(pwd)/target/.contract.lock"
# A lock left behind by a killed process would block every later build forever, so
# one older than any plausible generation is taken as abandoned.
if [ -d "$LOCK" ] && [ -z "$(find "$LOCK" -maxdepth 0 -mmin -20 2>/dev/null)" ]; then
    echo "removing an abandoned $LOCK"
    rmdir "$LOCK" 2>/dev/null || true
fi
waited=0
while ! mkdir "$LOCK" 2>/dev/null; do
    waited=$((waited + 1))
    if [ "$waited" -gt 600 ]; then
        echo "timed out waiting for $LOCK"
        exit 1
    fi
    sleep 1
done
trap 'rmdir "$LOCK" 2>/dev/null || true' EXIT

# Re-checked while holding the lock: the process we queued behind was very likely
# generating exactly what we were about to.
if [ "$IF_NEEDED" = "1" ] && up_to_date; then
    exit 0
fi

# Checked here rather than at the top: --if-needed returns above without running
# maven, and the local Java SE loop should not demand a JDK 8 it never uses.
J8="${JDK_8_HOME:?set JDK_8_HOME to a JDK 8 home}"

# The contract compiles against codenameone-core and is processed by the Codename
# One maven plugin, so both have to be in the local repo. Saying which ones are
# missing beats maven's "could not resolve" on an artifact nobody asked for
# directly -- this is the first thing a fresh checkout hits.
CN1_VERSION="$(sed -n 's/.*<cn1\.version>\(.*\)<\/cn1\.version>.*/\1/p' contract/pom.xml | head -1)"
for artifact in codenameone-core codenameone-maven-plugin; do
    if [ ! -d "$M2/com/codenameone/$artifact/$CN1_VERSION" ]; then
        echo "$artifact:$CN1_VERSION is not in $M2."
        echo "Install it first:"
        echo "  (cd $REPO/maven && JAVA_HOME=\$JDK_8_HOME mvn -B -pl core,codenameone-maven-plugin \\"
        echo "      -am install -DskipTests -Plocal-dev-javase -Dmaven.repo.local=$M2)"
        exit 1
    fi
done

JAVA_HOME="$J8" mvn -q -B -f contract/pom.xml process-classes \
    -Dcn1.restServer=true -Dmaven.repo.local="$M2"

rm -rf gen && mkdir -p gen
cp -r contract/target/classes/. gen/
# The same goal generates BOTH halves. The client half -- <Api>Impl and
# cn1app.RestClientBootstrap -- belongs to the app: it calls
# com.codename1.io.rest.Rest, which needs a CodenameOneImplementation the server
# does not have, so shipping it would break the backend link. The contract
# interface goes for the same reason (it names OnComplete and Response).
#
# What stays: <Api>Server, <Api>Dispatcher, the DTOs and their <Dto>Json codecs.
find gen -name '*Impl.class' -o -name '*Impl$*.class' | xargs -r rm -f
rm -rf gen/cn1app
# A contract type ships to the server exactly when a codec was generated for it:
# that is what makes it a DTO rather than the interface itself. Anything else from
# contract/ names OnComplete and Response and would not link.
for f in $(find contract -name '*.java'); do
    rel="${f#contract/}"
    cls="gen/${rel%.java}.class"
    codec="gen/${rel%.java}Json.class"
    if [ ! -f "$codec" ]; then
        rm -f "$cls" "gen/${rel%.java}"'$'*.class 2>/dev/null || true
    fi
done
echo "generated:"; find gen -name '*.class' | sort | sed 's/^/  /'
