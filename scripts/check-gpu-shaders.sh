#!/usr/bin/env bash
# Compiles every shader of com.codename1.gpu.shaders, and the post composite,
# with the real compilers: glslangValidator for GLSL ES 100 and for HLSL, and
# Apple's metal compiler where xcrun has one. A generator change that prints
# code one GPU rejects fails here instead of on that device, where the model
# simply does not draw.
#
# Needs core, ios and windows built (maven/*/target/classes) and
# glslangValidator on PATH; Metal is checked on macOS only.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CP="$ROOT/maven/core/target/classes:$ROOT/maven/ios/target/classes:$ROOT/maven/windows/target/classes"
for d in core ios windows; do
  [ -d "$ROOT/maven/$d/target/classes" ] || { echo "build maven/$d first" >&2; exit 2; }
done
command -v glslangValidator >/dev/null || { echo "glslangValidator is not on PATH" >&2; exit 2; }
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-gpu-shaders.XXXXXX")"
mkdir -p "$WORK/classes" "$WORK/out"
javac -nowarn -d "$WORK/classes" -cp "$CP" "$ROOT/scripts/gpu-shader-check/ShaderDump.java"
java -cp "$WORK/classes:$CP" ShaderDump "$WORK/out"
fail=0
count=0
for f in "$WORK"/out/*.vert "$WORK"/out/*.frag; do
  count=$((count + 1))
  if ! out=$(glslangValidator "$f" 2>&1); then
    echo "GLSL $f"; echo "$out" | grep -m5 -i error || true; fail=1
  fi
done
for f in "$WORK"/out/*.hlsl; do
  stages="frag:cn1_fragment_main"
  case "$f" in */post.hlsl) ;; *) stages="$stages vert:cn1_vertex_main" ;; esac
  for s in $stages; do
    count=$((count + 1))
    if ! out=$(glslangValidator -D -V --auto-map-bindings -o /dev/null -S "${s%%:*}" -e "${s#*:}" "$f" 2>&1); then
      echo "HLSL $f ${s%%:*}"; echo "$out" | grep -m5 -i error || true; fail=1
    fi
  done
done
if command -v xcrun >/dev/null && xcrun -sdk iphoneos -f metal >/dev/null 2>&1; then
  for f in "$WORK"/out/*.metal; do
    count=$((count + 1))
    if ! out=$(xcrun -sdk iphoneos metal -c "$f" -o /dev/null 2>&1); then
      echo "MSL $f"; echo "$out" | grep -m5 error || true; fail=1
    fi
  done
else
  echo "no Metal compiler here; MSL not checked"
fi
echo "$count shader compiles, $( [ $fail = 0 ] && echo clean || echo FAILED )"
exit $fail
