# Sourced, not executed: compiles a translator-emitted clean-target dist into one
# native executable. Shared by build-selfhost.sh (the self-hosted translator) and
# build-bench.sh (the Bench microbenchmarks), so both are built exactly the same way on
# every platform -- a benchmark binary compiled differently from the translator would
# measure the difference in flags rather than the VM.
#
# The caller defines REPO, WINDOWS, PYTHON, CC and native_path (build-selfhost.sh does),
# and optionally CN1_SELFHOST_CFLAGS / CN1_SELFHOST_CFLAGS_WINDOWS. When
# CN1_BUILD_SOURCE_SNAPSHOT is set the binary also gets a .build.json manifest, which is
# what lets perf-gate.py refuse a stale binary.
#
#   cn1_compile_dist <distDir> <AppName> <outBinary> <-O1|-O3> <logFile>
#
# The mandatory clang flags are not negotiable for generated C: Java arithmetic wraps,
# and clang -O3 provably miscompiles without -fwrapv -fno-strict-aliasing
# -fno-builtin-fmod(f). See vm/benchmarks/README.md.
cn1_compile_dist() {
    local dist="$1" app="$2" bin="$3" opt="$4" log="$5"
    local srcdir="$dist/$app-src"
    if [ "$WINDOWS" = 1 ]; then
        # The translator's own CMake project, turned into an executable the way
        # CleanTargetIntegrationTest does it, so this compiles exactly what that test
        # proves compiles on Windows. Release plus /clang:$opt; no ThinLTO, which would
        # need lld-link and is not what the Windows builder ships.
        "$PYTHON" - "$dist/CMakeLists.txt" <<'PYEOF'
import sys
path = sys.argv[1]
text = open(path, encoding='utf-8').read()
if 'add_executable(${PROJECT_NAME}' not in text:
    at = text.index('add_library(${PROJECT_NAME}')
    end = text.index(')', at)
    text = text[:at] + 'add_executable(' + text[at + len('add_library('):end + 1] + text[end + 1:]
open(path, 'w', encoding='utf-8').write(text)
PYEOF
        local build="$dist/../cmake-build"
        rm -rf "$build"
        cmake -S "$(native_path "$dist")" -B "$(native_path "$build")" -G Ninja \
            -DCMAKE_C_COMPILER=clang-cl -DCMAKE_BUILD_TYPE=Release \
            "-DCMAKE_C_FLAGS_RELEASE=/O2 /Ob2 /DNDEBUG /clang:$opt $CN1_SELFHOST_CFLAGS_WINDOWS" \
            > "$log" 2>&1 \
            && cmake --build "$(native_path "$build")" >> "$log" 2>&1 \
            || { echo "COMPILE FAILED"; tail -40 "$log"; return 1; }
        cp "$build/$app.exe" "$bin"
        if [ -n "${CN1_BUILD_SOURCE_SNAPSHOT:-}" ]; then
            CN1_BUILD_FLAGS="Release /clang:$opt $CN1_SELFHOST_CFLAGS_WINDOWS" CN1_SELFHOST_CC=clang-cl \
                "$PYTHON" "$REPO/vm/selfhost/bench-selfhost.py" --record-build "$bin"
        fi
        echo "built $bin"
        return 0
    fi
    # The .S as well as the .c: the virtual-thread context switch is emitted beside the
    # generated sources and the C half references it, so a *.c-only invocation links
    # against a missing cn1VirtualThreadSwitch.
    local asms
    asms=$(ls "$srcdir"/*.S 2>/dev/null || true)
    # -flto=thin for -O3 arrives through CN1_SELFHOST_CFLAGS, which the caller sets.
    local flags="$opt"
    $CC $flags -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
        $CN1_SELFHOST_CFLAGS -I"$srcdir" "$srcdir"/*.c $asms -lm -lpthread -o "$bin" \
        2> "$log" || { echo "COMPILE FAILED"; tail -40 "$log"; return 1; }
    if [ -n "${CN1_BUILD_SOURCE_SNAPSHOT:-}" ]; then
        CN1_BUILD_FLAGS="$flags -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf $CN1_SELFHOST_CFLAGS" \
            "$PYTHON" "$REPO/vm/selfhost/bench-selfhost.py" --record-build "$bin"
    fi
    echo "built $bin"
}
