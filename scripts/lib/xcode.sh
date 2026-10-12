# shellcheck shell=bash
#
# Select the Xcode the iOS/macOS/Catalyst scripts build with.
#
# This used to be a twelve-line prologue copy-pasted into eight scripts and
# inlined again in three workflows, every copy hardcoding "Xcode_26*". Moving to
# the next Xcode meant eleven identical edits, and the copies had already drifted
# apart in their exit codes and their log prefixes.
#
# Resolution order:
#
#   1. $XCODE_APP                -- an explicit choice always wins, and is never
#                                   second-guessed on version. This is how you
#                                   try a newer Xcode before CI can.
#   2. $CN1_XCODE_MAJOR          -- the pinned major (default below), chosen from
#                                   every Xcode under /Applications by the version
#                                   its own xcodebuild reports. That covers
#                                   Xcode_26.1.app (the GitHub runner images),
#                                   Xcode27.app (a hand-installed beta) and plain
#                                   Xcode.app (an ordinary install) without
#                                   caring which of those a machine happens to
#                                   use. Newest matching version wins.
#   3. nothing                   -- fail, loudly, naming what was found instead.
#
# There is deliberately no fall-back to the ambient Xcode. Silently building
# with whatever xcode-select happens to point at is the opposite of a pin, and
# it is how a screenshot baseline gets reseeded from a different toolchain.
#
# Versions are read from `xcodebuild -version`, never inferred from the bundle
# name. A directory name is a claim; the binary is the answer, and they disagree
# often enough to matter -- a renamed bundle, a symlink, a beta installed over a
# release. Selecting on the reported version is also what makes it safe to
# consider plain /Applications/Xcode.app: it is accepted when it IS the pinned
# major and ignored when it is not, so the pin still means something. Globbing
# for "Xcode_26*" instead, as this used to, missed an ordinary install outright
# and would have trusted the name if it found one.

# The Xcode major the tree is validated against. Raising this is the one edit a
# toolchain move needs; every script and workflow reads it from here.
: "${CN1_XCODE_MAJOR:=26}"

# Usage: cn1_select_xcode [log_function]
# Exports DEVELOPER_DIR, XCODEBUILD, PATH and CN1_XCODE_VERSION on success.
# Returns non-zero on failure so the caller keeps its own exit code.
cn1_select_xcode() {
    local log="${1:-echo}"
    local explicit="no"
    local candidate=""

    if [ -n "${XCODE_APP:-}" ]; then
        explicit="yes"
        candidate="$XCODE_APP"
        if [ ! -x "$candidate/Contents/Developer/usr/bin/xcodebuild" ]; then
            "$log" "XCODE_APP=$XCODE_APP does not contain Contents/Developer/usr/bin/xcodebuild." >&2
            return 1
        fi
    else
        local app version major best_version="" seen=""
        for app in /Applications/Xcode*.app; do
            [ -x "$app/Contents/Developer/usr/bin/xcodebuild" ] || continue
            # Run it, then judge it on BOTH its output and its exit status, and
            # let neither kill the caller.
            #
            # Measured on the GitHub xcode-27 image, where /Applications holds the
            # real Xcode plus three symlinks to it:
            #
            #   Xcode_27_Release_Candidate.app -> prints "Xcode 27.0", exit 134
            #   Xcode_27.0.0.app / Xcode_27.0.app / Xcode.app -> 27.0, exit 0
            #
            # Two separate traps in that one line. The crashing app is FIRST in
            # glob order, and without the `|| true` the failing status propagates
            # out of the assignment and `set -e` -- which every CI step here runs
            # with -- kills selection on candidate one, before any working Xcode
            # is tried, with no output at all because the crash text went to
            # /dev/null. And it prints a perfectly good version BEFORE aborting,
            # so checking the string alone accepts an Xcode whose xcodebuild dies;
            # here that was survivable only because all four report 27.0 and the
            # tie-break happens to keep the last, which is luck rather than logic.
            #
            # An xcodebuild that cannot exit cleanly is not one to build with.
            version="$("$app/Contents/Developer/usr/bin/xcodebuild" -version 2>/dev/null \
                        | awk '/^Xcode /{print $2; exit}' || true)"
            if ! "$app/Contents/Developer/usr/bin/xcodebuild" -version >/dev/null 2>&1; then
                seen="$seen ${version:-?}($app, unusable)"
                continue
            fi
            [ -n "$version" ] || continue
            seen="$seen $version($app)"
            major="${version%%.*}"
            [ "$major" = "$CN1_XCODE_MAJOR" ] || continue
            # sort -V so 26.10 beats 26.9, which a string compare gets backwards.
            if [ -z "$best_version" ] || \
               [ "$(printf '%s\n%s\n' "$best_version" "$version" | sort -V | tail -n 1)" = "$version" ]; then
                best_version="$version"
                candidate="$app"
            fi
        done
        if [ -z "$candidate" ]; then
            "$log" "No Xcode $CN1_XCODE_MAJOR under /Applications. Found:${seen:- none}." >&2
            "$log" "Set XCODE_APP to an installed Xcode, or CN1_XCODE_MAJOR to a version you have." >&2
            return 1
        fi
    fi

    export DEVELOPER_DIR="$candidate/Contents/Developer"
    export XCODEBUILD="$DEVELOPER_DIR/usr/bin/xcodebuild"
    export PATH="$DEVELOPER_DIR/usr/bin:$PATH"

    CN1_XCODE_VERSION="$("$XCODEBUILD" -version 2>/dev/null | awk '/^Xcode /{print $2; exit}')"
    export CN1_XCODE_VERSION
    if [ -z "$CN1_XCODE_VERSION" ]; then
        "$log" "$XCODEBUILD did not report a version; the install looks incomplete." >&2
        return 1
    fi

    local actual_major="${CN1_XCODE_VERSION%%.*}"
    if [ "$explicit" = "yes" ]; then
        # An explicit override is the escape hatch, so a mismatch is reported and
        # allowed rather than refused -- but it is reported, because a build that
        # quietly used a different toolchain than the pin is worth seeing in a log.
        if [ "$actual_major" != "$CN1_XCODE_MAJOR" ]; then
            "$log" "XCODE_APP selects Xcode $CN1_XCODE_VERSION, not the pinned major $CN1_XCODE_MAJOR."
        fi
    elif [ "$actual_major" != "$CN1_XCODE_MAJOR" ]; then
        # Unreachable: the search above already selected on the reported version.
        # Kept so a future change to the search cannot quietly hand back the
        # wrong major.
        "$log" "$candidate reports Xcode $CN1_XCODE_VERSION, not the pinned major $CN1_XCODE_MAJOR." >&2
        return 1
    fi

    "$log" "Using Xcode $CN1_XCODE_VERSION"
    "$log" "Using DEVELOPER_DIR=$DEVELOPER_DIR"
    "$log" "Using XCODEBUILD=$XCODEBUILD"
    return 0
}

# Usage: cn1_watch_simulator_arch_xcconfig >> some.xcconfig
#
# Prints the xcconfig lines that hold an EMBEDDED watch app to one simulator
# slice. For CI's own builds of the sample; nothing a customer runs reads this,
# and the generated project is untouched.
#
# The generated watch target says ONLY_ACTIVE_ARCH=YES and
# ARCHS[sdk=watchsimulator*]=$(ARCHS_STANDARD), which is one slice when the
# destination is a watch. Built as a dependency of the iOS scheme the
# destination is an iPhone, the watch platform has no active architecture, and
# xcodebuild compiles the watch app for arm64 AND x86_64. Measured on the Metal
# screenshot legs: 14,834 compile units of which 9,880 were the watch app --
# 4,940 per slice -- against 4,943 for the iOS app the leg exists to test, and
# 21 to 34 minutes of a 33 to 53 minute build. Nothing installs or launches the
# embedded copy, and the watch job builds its own single slice with -arch arm64.
#
# These lines must reach xcodebuild through -xcconfig (or XCODE_XCCONFIG_FILE),
# for two reasons. A command-line setting cannot carry the [sdk=...] condition
# at all -- see run-ios-ui-tests.sh, where that was found the hard way -- and an
# unconditioned ARCHS would land on the iOS app as well. And the target sets
# this very condition itself, so the override has to outrank a target-level
# setting: `man xcodebuild` says of -xcconfig that "these settings will override
# all other settings, including settings passed individually on the command
# line". Checked rather than taken on trust, with -showBuildSettings on the
# generated project under Xcode 26.3 and Xcode 27:
#
#   HelloCodenameOneWatch, CN1WatchWidgets / watchsimulator   arm64 x86_64 -> arm64
#   HelloCodenameOneWatch / watchos                           arm64_32, unchanged
#   HelloCodenameOne / iphonesimulator                        arm64 x86_64, unchanged
#
# EXCLUDED_ARCHS says the same thing a second way. No generated target sets it
# for the watch simulator, so it holds whichever way the precedence falls.
#
# The warning census is unaffected: it keys a diagnostic on its own location,
# never on the architecture that reported it, and holds a baseline as a set of
# kinds, not of counts. In three full logs across both toolchains exactly one
# diagnostic came from the x86_64 slice alone (unused function
# 'aesHardwareAvailable' in the SQLite amalgamation), and its kind -- file, flag
# and message shape -- is reported by the arm64 slice for the same file, so no
# baseline entry goes stale and none needs regenerating.
cn1_watch_simulator_arch_xcconfig() {
    local host other
    host="$(uname -m 2>/dev/null || echo arm64)"
    case "$host" in
        x86_64) other="arm64" ;;
        *) host="arm64"; other="x86_64" ;;
    esac
    printf 'ARCHS[sdk=watchsimulator*] = %s\n' "$host"
    printf 'EXCLUDED_ARCHS[sdk=watchsimulator*] = %s\n' "$other"
}
