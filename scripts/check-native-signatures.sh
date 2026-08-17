#!/usr/bin/env bash
#
# Fails when a `native` method in one of our ParparVM-translated ports has no C
# implementation, or has one whose prototype does not match what the translator
# will call. ParparVM encodes the whole Java signature in the C function name --
# the "__" that opens the argument list, the per-argument "_", and the
# "_R_<returnType>" suffix -- and getting any of it wrong produces a function
# nothing links against, which the dead-code pass then reads as "this native
# method is unused". The build stays green and the feature is inert on device.
# See vm/ByteCodeTranslator/.../NativeSignatureVerifier for the full rules.
#
#   scripts/check-native-signatures.sh [--require-all] [--quiet-warnings]
#
# This is the offline half of the gate: it checks Codename One's own ports
# without needing a device build. Every real translation runs the same verifier
# over the generated project, which additionally covers the app, its cn1libs and
# the native-interface glue the builders inject.
#
# A port that has not been compiled is skipped with a note, so a partial local
# tree still gives a useful answer -- CI passes --require-all, because there a
# missing port means the gate silently stopped covering it.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
TRANSLATOR="$REPO_ROOT/vm/ByteCodeTranslator/target/classes"
ASM_CP_FILE="$REPO_ROOT/vm/ByteCodeTranslator/target/native-signature-asm-classpath.txt"

require_all=0
quiet_warnings=0
for arg in "$@"; do
  case "$arg" in
    --require-all) require_all=1 ;;
    --quiet-warnings) quiet_warnings=1 ;;
    *) echo "check-native-signatures: unknown argument: $arg" >&2; exit 2 ;;
  esac
done

if [[ ! -f "$TRANSLATOR/com/codename1/tools/translator/NativeSignatureVerifier.class" ]]; then
  echo "check-native-signatures: building the translator" >&2
  (cd "$REPO_ROOT/vm" && mvn -q -B -pl ByteCodeTranslator -am package -DskipTests)
fi
if [[ ! -f "$ASM_CP_FILE" ]]; then
  (cd "$REPO_ROOT/vm" && mvn -q -B -pl ByteCodeTranslator \
     dependency:build-classpath "-Dmdep.outputFile=$ASM_CP_FILE")
fi

# Each port pairs the classes ParparVM translates with the native sources the
# generated project compiles alongside them. vm/JavaAPI and vm/ByteCodeTranslator/src
# (which holds nativeMethods.m and cn1_globals.m) are common to all three.
#
# Not covered: Android and JavaSE run on a real JVM with JNI, whose own name
# mangling is enforced by javah/the JNI linker rather than by this scheme.
PORTS=(
  "ios|maven/ios/target/classes|Ports/iOSPort/nativeSources"
  "windows|maven/windows/target/classes|Ports/WindowsPort/nativeSources"
  "linux|maven/linux/target/classes|Ports/LinuxPort/nativeSources"
)
COMMON_CLASSES=("vm/JavaAPI/target/classes" "maven/core/target/classes")
COMMON_NATIVES=("vm/ByteCodeTranslator/src")

status=0
checked=0
missing_port=0

for entry in "${PORTS[@]}"; do
  IFS='|' read -r name port_classes port_natives <<< "$entry"
  args=()
  ready=1
  for dir in "${COMMON_CLASSES[@]}" "$port_classes"; do
    if [[ -d "$REPO_ROOT/$dir" ]]; then
      args+=(--classes "$REPO_ROOT/$dir")
    else
      echo "check-native-signatures: skipping $name ($dir is not built)" >&2
      ready=0
      missing_port=1
      break
    fi
  done
  [[ "$ready" -eq 1 ]] || continue
  for dir in "${COMMON_NATIVES[@]}" "$port_natives"; do
    args+=(--natives "$REPO_ROOT/$dir")
  done
  if [[ "$quiet_warnings" -eq 1 ]]; then
    args+=(--no-orphans)
  fi

  echo "== $name"
  if ! java -cp "$TRANSLATOR:$(cat "$ASM_CP_FILE")" \
       com.codename1.tools.translator.NativeSignatureVerifier "${args[@]}"; then
    status=1
  fi
  checked=$((checked + 1))
done

if [[ "$require_all" -eq 1 && "$missing_port" -eq 1 ]]; then
  echo "check-native-signatures: --require-all was passed but a port is not built;" >&2
  echo "  the gate would have covered less than it claims. Build it and re-run." >&2
  exit 2
fi
if [[ "$checked" -eq 0 ]]; then
  echo "check-native-signatures: nothing to check -- no port is built" >&2
  exit 2
fi
exit "$status"
