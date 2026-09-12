#!/usr/bin/env bash
#
# Classifies the compiler warnings in a native build log by who owns the code.
#
# A ParparVM build compiles four different kinds of C into one binary -- the
# translator's output, the ParparVM runtime, the hand-written port natives, and
# vendored third-party sources -- and their warnings arrive in one log with
# nothing to tell them apart. At that volume a real defect (an Apple API that is
# deprecated now and deleted in two releases, a pointer/integer confusion in
# generated code) is invisible. Ownership cannot be recovered from the path,
# because all four end up in the same flat directory; it comes from the manifest
# the translator writes beside the generated project.
#
# The parser self-test needs no compiler and is what PR CI runs:
#
#   scripts/check-native-warnings.sh --self-test
#
# A census needs a log and the manifest from the SAME build:
#
#   scripts/check-native-warnings.sh --leg ios-sim-debug \
#       --log artifacts/xcodebuild-build.log \
#       --manifest artifacts/cn1-source-manifest.txt
#
# Add --report-only to print the census without gating, --write-baseline to
# record the current findings, and --probe to assert the gate still reacts to an
# injected warning.
#
# Exit 2 means the build did not compile everything the manifest lists, so the
# census would undercount -- an incremental build reports no warnings and reads
# exactly like a clean codebase. Re-run against a cold build rather than
# believing the number.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec python3 "$SCRIPT_DIR/check-native-warnings.py" "$@"
