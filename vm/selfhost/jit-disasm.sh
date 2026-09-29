#!/bin/bash
# ============================================================================
# What did HotSpot's JIT actually emit for a method, so it can be read beside
# the C we generate for the same one.
#
# This needs hsdis, which no JDK ships. The JDK's own source tree carries the
# backends (src/utils/hsdis) and a capstone build is three flags; the README
# there describes a configure/make route that is not needed for one dylib.
#
#   jit-disasm.sh <classpath> <MainClass> <Class::method> [args...]
#
# Note what you are reading. C2 output is the STEADY STATE after warmup and
# after speculative optimizations that our AOT output cannot take without the
# same profile -- uncommon traps, monomorphic inline caches on a receiver that
# happened to be stable. Comparing instruction COUNTS is therefore misleading;
# what transfers is what it chose to inline, what it hoisted, and which checks
# it eliminated entirely.
# ============================================================================
set -euo pipefail
: "${JDK_25_HOME:?set JDK_25_HOME}"
CP="$1"; MAIN="$2"; METHOD="$3"; shift 3
HSDIS="$JDK_25_HOME/lib/hsdis-aarch64.dylib"
if [ ! -f "$HSDIS" ]; then
    echo "hsdis not installed at $HSDIS" >&2
    echo "build it:  clang -O2 -shared -fPIC -o \$JDK_25_HOME/lib/hsdis-aarch64.dylib \\" >&2
    echo "    -DCAPSTONE_ARCH=CS_ARCH_ARM64 -DCAPSTONE_MODE=CS_MODE_ARM \\" >&2
    echo "    -I<jdk-src>/src/utils/hsdis -I\$(brew --prefix capstone)/include/capstone \\" >&2
    echo "    -I\$(brew --prefix capstone)/include -I\$JDK_25_HOME/include -I\$JDK_25_HOME/include/darwin \\" >&2
    echo "    <jdk-src>/src/utils/hsdis/capstone/hsdis-capstone.c -L\$(brew --prefix capstone)/lib -lcapstone" >&2
    exit 1
fi
exec "$JDK_25_HOME/bin/java" -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly \
    -XX:CompileCommand=print,"$METHOD" -cp "$CP" "$MAIN" "$@"
