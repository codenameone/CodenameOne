# shellcheck shell=bash
#
# Resolve the iOS simulator a fidelity capture must run on, from the golden set.
#
# A golden set names an OS DESIGN GENERATION (ios-26-metal, ios-27-metal), and a
# capture has to run on a runtime of that generation: scoring iOS 27 renders
# against iOS 26 references -- or the reverse -- produces phantom regressions out
# of nothing but a different SF font revision.
#
# This exists because the capture scripts each carried the same hardcoded UDID
# (17853196-...), which on this machine names a device called "iPhone16-iOS26".
# A literal UDID says nothing about which runtime it is on, so the one thing the
# golden set demands was the one thing the default could not express, and moving
# to a second generation meant editing the literal in two scripts and hoping.
#
# Matching is on the device TYPE identifier, never the device name: the device
# the ios-26-metal set was captured on is named "iPhone16-iOS26", which no
# pattern derived from "iPhone 16" would find. The model must stay the same
# across generations, or a 26-vs-27 comparison also picks up a screen-size
# change and reports it as a design change.
#
# Why the search is "every runtime of this major that HAS the device type, newest
# first" rather than "the newest runtime of this major":
#
#     iOS 27.1 supports exactly ONE device type -- iPhone Duo.
#
# Measured, not assumed: `simctl list runtimes --json` reports 65 supported
# device types for iOS 26.3, 62 for iOS 27.0, and 1 for iOS 27.1. It is a
# foldable bring-up runtime; `simctl create` answers "Incompatible device"
# (SimError 403) for iPhone 16, 16 Pro, 17, 17 Pro, Air and 18 Pro alike, under
# Xcode 27.1's own simctl. So the golden capture for ios-27-metal necessarily
# runs on iOS 27.0, where iPhone 16 still exists, and iOS 27.1 is where the
# hinge APIs get exercised on an iPhone Duo. Filtering by device type BEFORE
# taking the newest runtime is what makes that fall out correctly instead of
# failing with "no iPhone 16 on iOS 27".

# The model every iOS golden set is captured on. Override only to capture a new
# set on a different model -- and then never compare it against an old one.
: "${NATIVEREF_DEVICE_TYPE:=com.apple.CoreSimulator.SimDeviceType.iPhone-16}"

# Usage: cn1_resolve_ios_sim_udid <golden_set> [log_function]
# Prints the UDID on stdout; diagnostics go to stderr. Returns non-zero when no
# device of the right type exists on a runtime of the right generation, so the
# caller fails instead of capturing against the wrong OS.
cn1_resolve_ios_sim_udid() {
    local golden_set="$1"
    local log="${2:-echo}"
    local major
    case "$golden_set" in
        ios-26-*) major=26 ;;
        ios-27-*) major=27 ;;
        *)
            "$log" "Cannot derive an iOS runtime from golden set '$golden_set'; pass a UDID." >&2
            return 1
            ;;
    esac
    if xcrun simctl list devices available --json \
        | CN1_WANT_MAJOR="$major" CN1_WANT_TYPE="$NATIVEREF_DEVICE_TYPE" python3 -c '
import json, os, re, sys

want_major = os.environ["CN1_WANT_MAJOR"]
want_type = os.environ["CN1_WANT_TYPE"]
candidates = []
for runtime, devices in json.load(sys.stdin)["devices"].items():
    m = re.search(r"SimRuntime\.iOS-(\d+)(?:-(\d+))?$", runtime)
    if not m or m.group(1) != want_major:
        continue
    minor = int(m.group(2) or 0)
    for device in devices:
        if device.get("deviceTypeIdentifier") == want_type:
            candidates.append((minor, device["name"], device["udid"], runtime))
if not candidates:
    sys.exit(1)
# Newest minor wins -- a machine with both 27.0 and 27.1 captures on 27.1, which
# is where the foldable APIs and the iPhone Duo live. Ties break on (name, udid)
# rather than dictionary order so the same machine picks the same device every
# run: several identically-typed devices on one runtime is the normal state of a
# working machine (this one has four), and a capture that silently hopped
# between them would make a golden set unreproducible.
candidates.sort(key=lambda c: (-c[0], c[1], c[2]))
chosen = candidates[0]
sys.stdout.write(chosen[2] + "\n")
sys.stderr.write("[ios-sim] %s on %s (%s)\n" % (chosen[1], chosen[3], chosen[2]))
if len(candidates) > 1:
    sys.stderr.write("[ios-sim] %d devices of this type are available; picked the first by\n"
                     % len(candidates))
    sys.stderr.write("[ios-sim] (newest runtime, name, udid). Pass a UDID to choose another.\n")
'
    then
        return 0
    fi
    "$log" "No available device of type $NATIVEREF_DEVICE_TYPE on an iOS $major runtime," >&2
    "$log" "which is what golden set '$golden_set' has to be captured on. Create one against a" >&2
    "$log" "runtime that supports the type -- not every runtime supports every device:" >&2
    "$log" "  xcrun simctl list runtimes --json   # read supportedDeviceTypes" >&2
    "$log" "  xcrun simctl create iPhone16-iOS$major $NATIVEREF_DEVICE_TYPE <runtime-id>" >&2
    "$log" "and download the runtime first if it is absent: xcodebuild -downloadPlatform iOS" >&2
    return 1
}
