#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CN1_DIR="$ROOT_DIR/CodenameOne"

JDK_HOME="${JDK_25_HOME:-${JAVA_HOME:-}}"
if [ -n "$JDK_HOME" ] && [ -x "$JDK_HOME/bin/javadoc" ]; then
  JAVADOC_CMD="$JDK_HOME/bin/javadoc"
else
  JAVADOC_CMD="javadoc"
fi

run_with_timeout() {
  local seconds="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "${seconds}" "$@"
    return $?
  fi
  "$@"
}

rm -rf "$CN1_DIR/dist/javadoc"
rm -rf "$CN1_DIR/build/tempJavaSources"

mkdir -p "$CN1_DIR/build/tempJavaSources"
mkdir -p "$CN1_DIR/dist/javadoc"

cp -r "$CN1_DIR/src/"* "$CN1_DIR/build/tempJavaSources/"

VALIDATE_SNIPPETS_SCRIPT="${SCRIPT_DIR}/validate-extracted-javadoc-snippets.sh"
if [ -x "${VALIDATE_SNIPPETS_SCRIPT}" ]; then
  echo "Validating JavaDoc snippets prior to JavaDoc generation..." >&2
  if ! run_with_timeout 300 "${VALIDATE_SNIPPETS_SCRIPT}"; then
    status=$?
    if [ "${status}" -eq 124 ]; then
      echo "JavaDoc snippet validation timed out after 300 seconds." >&2
    fi
    exit "${status}"
  fi
fi

cat > "$CN1_DIR/build/tempJavaSources/com/codename1/impl/ImplementationFactory.java" <<'EOF'
package com.codename1.impl;

public class ImplementationFactory {
    public static ImplementationFactory getInstance() {
        return null;
    }

    public Object createImplementation() {
        return null;
    }
}
EOF

# Collect every source file into a javadoc @argfile and run javadoc exactly
# once. Piping the list through `xargs javadoc -d <dir>` is unsafe: once the
# command line exceeds the shell/xargs limit (~128 KiB on GNU xargs) the list is
# split across multiple javadoc invocations, each regenerating the same output
# directory. The last batch (the Ports/CLDC11 java.* sources) then clobbers the
# full API docs, leaving only the CLDC packages. An @argfile has no length limit.
#
# The internal implementation package com.codename1.impl (and every sub package
# such as com.codename1.impl.gpu) is never part of the published API. The
# javadoc `-exclude` option only filters packages discovered through
# `-subpackages`; with an explicit source @argfile it has no effect, so we drop
# the impl sources from the list here. They remain reachable for symbol
# resolution through `-sourcepath` below, so references from documented classes
# still resolve without documenting impl itself.
SOURCES_ARGFILE="$CN1_DIR/build/javadoc-sources.txt"
find "$CN1_DIR/build/tempJavaSources" "$ROOT_DIR/Ports/CLDC11/src" -name "*.java" \
  | grep -v '/com/codename1/impl/' > "$SOURCES_ARGFILE"

# Held to doclint, and a failure fails the build: no -Xdoclint:none and no
# "|| true". An unresolved [Reference] is not cosmetic here -- the website's
# doclet renders it as plain text without a word, so every one is a link that
# silently vanished from the published page. -Werror makes a warning (a
# duplicated @param, an {@inheritDoc} with nothing to inherit) fail it too.
# "missing" stays off: requiring a
# comment on every member of the API is a different project.
#
# @warning is the vendored JBox2D's safety-note tag (22 uses). It is registered
# rather than rewritten, so this archive prints it under a "Warning:" heading and
# the website's doclet keeps lifting it into a warning block of its own.
"$JAVADOC_CMD" \
  --allow-script-in-comments \
  --add-stylesheet "$ROOT_DIR/maven/javadoc-resources/highlight.css" \
  --add-script "$ROOT_DIR/maven/javadoc-resources/highlight.min.js" \
  --add-script "$ROOT_DIR/maven/javadoc-resources/javadoc-highlight-init.js" \
  --release 8 \
  -sourcepath "$CN1_DIR/build/tempJavaSources:$ROOT_DIR/Ports/CLDC11/src" \
  -exclude com.codename1.impl \
  -Xdoclint:all,-missing \
  -Xmaxerrs 10000 \
  -Xmaxwarns 10000 \
  -Werror \
  -tag "warning:a:Warning:" \
  -quiet \
  -protected \
  -d "$CN1_DIR/dist/javadoc" \
  -windowtitle "Codename One API" \
  "@$SOURCES_ARGFILE"

# Fail loudly if the core API failed to generate. Without this guard a partial
# build (e.g. only the CLDC java.* packages) ships silently to the website.
if [ ! -f "$CN1_DIR/dist/javadoc/com/codename1/ui/Component.html" ]; then
  echo "JavaDoc generation produced no core com.codename1.ui output; aborting." >&2
  exit 1
fi

# Guard: com.codename1.impl is an internal package and must never reach the
# published API. If a regression documents it anyway, fail loudly here rather
# than shipping internal classes to the website.
if [ -e "$CN1_DIR/dist/javadoc/com/codename1/impl" ]; then
  echo "JavaDoc generated com.codename1.impl output; the internal implementation package must stay excluded." >&2
  exit 1
fi

(
  cd "$CN1_DIR/dist/javadoc"
  zip -r "$CN1_DIR/javadocs.zip" .
)

# ---------------------------------------------------------------------------
# The backend API: the server runtime under vm/backend, published as a reference
# of its own (backend-javadocs.zip here, /backend/javadoc/ on the website) so a
# reader never mistakes a server class for one the app can call.
#
# Its sources are staged from three places: the shared runtime (vm/backend/src),
# the per-target classes, and the core classes marked
# @com.codename1.impl.SharedWithBackend, which vm/backend/shared-sources.sh
# lists. Those shared classes are documented in BOTH references, because both
# halves of an application really do use them.
#
# The per-target classes come from impl/parparvm, the production runtime, and
# not from the impl/javase twins the local Maven jar compiles. The two have the
# same public surface by design, but only the production classes document what
# a class IS -- each Java SE twin documents how it differs from the real one
# ("Java SE twin of Crypto, on the JDK's own providers"), which is a note for
# whoever maintains it and nonsense as the summary of a public API page. They
# declare nothing outside java.*, so the JDK resolves them here as it does the
# rest.
#
# The shared classes keep their package-info.java, so a shared package has the
# same description in both references. SharedWithBackend itself and the rest of
# com.codename1.impl are staged for symbol resolution and filtered out of the
# documented set, exactly as for the client API above.
BACKEND_DIR="$ROOT_DIR/vm/backend"
BACKEND_STAGE="$CN1_DIR/build/backendJavaSources"
rm -rf "$BACKEND_STAGE" "$CN1_DIR/dist/backend-javadoc" "$CN1_DIR/backend-javadocs.zip"
mkdir -p "$BACKEND_STAGE" "$CN1_DIR/dist/backend-javadoc"
cp -r "$BACKEND_DIR/src/." "$BACKEND_STAGE/"
cp -r "$BACKEND_DIR/impl/parparvm/." "$BACKEND_STAGE/"
while IFS= read -r shared; do
  rel="${shared#"$CN1_DIR/src/"}"
  mkdir -p "$BACKEND_STAGE/$(dirname "$rel")"
  cp "$shared" "$BACKEND_STAGE/$rel"
  info="$(dirname "$shared")/package-info.java"
  if [ -f "$info" ] && [ ! -f "$BACKEND_STAGE/$(dirname "$rel")/package-info.java" ]; then
    cp "$info" "$BACKEND_STAGE/$(dirname "$rel")/package-info.java"
  fi
done < <("$BACKEND_DIR/shared-sources.sh")

# The java.* classes the framework ships (Ports/CLDC11/src) are documented
# here too, read in place exactly as the client run reads them. They are shared:
# the backend compiles against vm/JavaAPI, which provides every public class and
# member of every one of them (it is a superset), so each page is as true for a
# server as for an app. The website's doclet marks the whole tree shared with
# --shared-sources.
BACKEND_SOURCES_ARGFILE="$CN1_DIR/build/backend-javadoc-sources.txt"
# The vm/JavaAPI classes the backend's public API exposes beyond the CLDC set --
# @Async's Future and what its get() throws, Config's Properties. The backend
# compiles against vm/JavaAPI, so these work there; listing them here is what makes
# them documented, supported API rather than an accident of the class library.
# check-backend-jdk-surface.py fails the build when the backend exposes a JDK type
# that is neither CLDC nor in this list. Keep the two lists in step.
BACKEND_PROMOTED_JDK="java/util/Properties.java
java/util/concurrent/CancellationException.java
java/util/concurrent/ExecutionException.java
java/util/concurrent/Future.java
java/util/concurrent/TimeUnit.java
java/util/concurrent/TimeoutException.java"
python3 "$ROOT_DIR/scripts/check-backend-jdk-surface.py"
{
  find "$BACKEND_STAGE" -name "*.java" | grep -v '/com/codename1/impl/'
  find "$ROOT_DIR/Ports/CLDC11/src" -name "*.java"
  echo "$BACKEND_PROMOTED_JDK" | while IFS= read -r promoted; do
    echo "$ROOT_DIR/vm/JavaAPI/src/$promoted"
  done
} | LC_ALL=C sort > "$BACKEND_SOURCES_ARGFILE"

# Held to the same doclint as the client API above. --release 8 matches the backend module's
# source level, and the JDK is the class library it compiles against.
"$JAVADOC_CMD" \
  --allow-script-in-comments \
  --add-stylesheet "$ROOT_DIR/maven/javadoc-resources/highlight.css" \
  --add-script "$ROOT_DIR/maven/javadoc-resources/highlight.min.js" \
  --add-script "$ROOT_DIR/maven/javadoc-resources/javadoc-highlight-init.js" \
  --release 8 \
  -sourcepath "$BACKEND_STAGE:$ROOT_DIR/Ports/CLDC11/src:$ROOT_DIR/vm/JavaAPI/src" \
  -Xdoclint:all,-missing \
  -Xmaxerrs 10000 \
  -Xmaxwarns 10000 \
  -Werror \
  -quiet \
  -protected \
  -d "$CN1_DIR/dist/backend-javadoc" \
  -windowtitle "Codename One Backend API" \
  "@$BACKEND_SOURCES_ARGFILE"

if [ ! -f "$CN1_DIR/dist/backend-javadoc/com/codename1/backend/HttpServer.html" ]; then
  echo "Backend JavaDoc generation produced no com.codename1.backend output; aborting." >&2
  exit 1
fi
if [ -e "$CN1_DIR/dist/backend-javadoc/com/codename1/impl" ]; then
  echo "Backend JavaDoc generated com.codename1.impl output; the internal package must stay excluded." >&2
  exit 1
fi
# The shared classes are the point of staging the core sources at all; a run
# that lost them would publish a backend ORM with no Session to call.
if [ ! -f "$CN1_DIR/dist/backend-javadoc/com/codename1/orm/session/Session.html" ]; then
  echo "Backend JavaDoc is missing the shared com.codename1.orm.session classes; aborting." >&2
  exit 1
fi

(
  cd "$CN1_DIR/dist/backend-javadoc"
  zip -r "$CN1_DIR/backend-javadocs.zip" .
)
