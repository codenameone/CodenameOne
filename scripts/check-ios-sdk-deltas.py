#!/usr/bin/env python3
"""Compile the iOS port's natives against two iOS SDKs and report what the newer one broke.

Why this exists
---------------
Apple can deprecate or delete an API, or change a behaviour that is gated on the SDK an app
was *linked* against, and nothing in this tree would notice: the port compiles against
whatever SDK the machine happens to have, and a warning that appears on every developer's
machine but not in CI is a warning nobody reads. iOS 27 alone deprecated
``[UIScreen displayLinkWithTarget:]``, ``[UIApplication canOpenURL:]`` and
``supportedInterfaceOrientationsForWindow:``, and silently turned ``setStatusBarStyle:``
into a no-op for anything linked against it.

How it works
------------
The same clang compiles the same bytes twice, with **only** ``-isysroot`` different. Any
diagnostic produced by an imperfect command line -- a missing include path, a header that
needs a translation unit we do not have here -- appears identically in both runs and
cancels out. What survives the subtraction is caused by the SDK and nothing else. This is
the same design, and the same reason it works, as ``check-android-api-removals.py``.

Two properties are load-bearing:

* **The feature gates are turned on, and they are ours to turn.** Most of the port's
  natives sit behind a ``//#define CN1_INCLUDE_X`` that ``IPhoneBuilder`` uncomments at
  build time. With the gates off, ``CN1SmartHome.m`` preprocesses to nothing and its live
  ``canOpenURL:`` call -- deprecated in iOS 27 -- is invisible. The gate list is harvested
  from the port sources rather than hand-written, so a gate added tomorrow is covered
  without editing this file.

  This is also why the sources compiled here are the **pristine** ones under
  ``Ports/iOSPort/nativeSources``, with a generated project supplying only the translated
  headers and the prefix header. In a generated tree the builder has already rewritten the
  gates for that one app -- the hello-world sample ships with ``CN1_USE_METAL`` uncommented
  -- and ``-D`` can only add a define, never remove one. Compiling the generated tree would
  silently test whichever configuration that sample happened to use, and report a GL sweep
  that was really a second Metal sweep.

* **The differ is proven to bite.** A synthetic probe that calls a known
  iOS-27-deprecated selector must show up as a delta. A comparison that silently produces
  nothing looks exactly like success, so the run fails if the probe does not fire.

The deployment-target trap
--------------------------
clang only warns about a deprecation once the **deployment target reaches the version the
API was deprecated in**. Compiled at ``-target arm64-apple-ios14.0`` -- the floor our
builds actually ship -- an SDK 27 build is completely silent about everything iOS 27
deprecated. Measured: ``[UIApplication canOpenURL:]`` against the iOS 27 SDK emits nothing
at ios14.0 and nothing at ios26.0, and warns only at ios27.0. A single sweep at the
shipping floor therefore reports "no new deprecations" no matter how many there are, which
is indistinguishable from a clean result.

So there are two sweeps, and they answer different questions:

* ``errors`` at the shipping deployment floor -- did the new SDK break the build our
  customers actually get? This is the one that blocks a release.
* ``deprecations`` at the new SDK's own version -- what have we inherited that will bite
  when the floor rises? Informational in the sense that nothing is broken today, but it is
  the only place these are visible at all, so they are reported rather than swallowed.
"""

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE_SOURCES = os.path.join(REPO_ROOT, "Ports", "iOSPort", "nativeSources")

# Names that appear as "//#define X" in the port sources but are not boolean feature gates.
# Everything else is turned on; see the module docstring for why the list is harvested
# rather than enumerated.
GATE_DENY = {
    # A function-like macro (a logging helper), not a gate.
    "CN1Log(str,...)",
    # The renderer selector, not a feature gate: CONFIGURATIONS owns it. Harvested along
    # with everything else it was defined for BOTH sweeps, so the "gl" run was a second
    # Metal run and a GL-only regression could not be seen. The renderer self-check missed
    # it because it built its defines differently from the sweeps -- which is why it now
    # builds them through the same helper.
    "CN1_USE_METAL",
    # A placeholder LINE that IPhoneBuilder replaces wholesale with one of the
    # CN1_METAL_COLORSPACE_* defines. Defining the placeholder itself selects nothing;
    # EXTRA_DEFINES below supplies the default the builder would have chosen.
    "CN1_METAL_COLORSPACE_PLACEHOLDER",
    # These pull in CocoaPods headers (FBSDK*, GoogleSignIn, MoPub, QBImagePicker) that
    # are not part of any SDK and are not in the generated tree unless the builder added
    # the pod. Left on, they do not merely add noise: the missing #import is a FATAL
    # error, so the translation unit stops there and every diagnostic after it -- the
    # ones this script exists to find -- is never emitted. Measured: with
    # ENABLE_GALLERY_MULTISELECT on, CodenameOne_GLViewController.h:496 fails to find
    # QBImagePickerController.h and the live iOS 27 canOpenURL: deprecation in
    # CN1SmartHome.m:2439 disappears from the output entirely.
    "ENABLE_GALLERY_MULTISELECT",
    "INCLUDE_FACEBOOK_CONNECT",
    "USE_FACEBOOK_CONNECT_PODS",
    "INCLUDE_GOOGLE_CONNECT",
    "GOOGLE_CONNECT_PODS",
    "GOOGLE_SIGNIN",
    "INCLUDE_MOPUB",
}

# ios.metal.colorSpace defaults to sRGB, so this is the define a default build really sees.
EXTRA_DEFINES = ["CN1_METAL_COLORSPACE_SRGB"]

# What a default build really ships. IPhoneBuilder adds 14.0 when ios.deployment_target is
# unset, over a DEFAULT_MIN_DEPLOYMENT_VERSION of 13.0 -- but it also raises to the SDK's own
# minimum, so under Xcode 27 a default app ships 15.0, not 14.0. Sampling 14.0 here would
# compile the port at a different __IPHONE_OS_VERSION_MIN_REQUIRED than the project that
# actually ships, and a regression visible only at the effective target would sit between
# both sampled endpoints and pass.
CN1_DEFAULT_DEPLOYMENT_TARGET = "14.0"

# Build configurations that select materially different code. The first is the baseline;
# each of the others is compiled as well, because a single pass would leave its half
# untested -- which is how a deprecation in the GL path survives a green Metal run.
#
# CN1_ON_DEVICE_DEBUG is here rather than in the harvested gate list because the builder
# does not uncomment it in the port sources: it rewrites the GENERATED cn1_globals.h. The
# harvester only reads pristine sources, so the debug bodies of cn1_debugger.m,
# cn1_debugger_objects.c and CodenameOne_GLAppDelegate.m compiled out of every sweep and an
# error there under a newer SDK would have passed unnoticed.
#
# Each non-baseline entry names a file that must preprocess differently under it. The
# configurations routinely produce identical DIAGNOSTICS, which is a fine result and also
# indistinguishable from one of them having quietly become a copy of the baseline, so the
# difference is checked directly.
CONFIGURATIONS = [
    ("gl", [], None),
    ("metal", ["CN1_USE_METAL"], "METALView.m"),
    ("ondevicedebug", ["CN1_ON_DEVICE_DEBUG"], "cn1_debugger.m"),
]

# A run that compiles almost nothing produces an empty delta, which reads as success. If
# fewer than this many files come back clean the harness itself is broken, not the SDK.
MIN_CLEAN_FILES = 50

# ParparVM emits one header per translated class that a native calls back into, so which
# of them exist depends on what the sample app happened to reference. A gate we turn on
# here can therefore #import a header the sample never caused to be generated -- and a
# missing #import is FATAL, which costs the whole rest of the file. Empty stubs are
# synthesized for these, and ONLY these: the name has to look like a translated class, so
# a genuinely missing SDK header still fails loudly instead of being papered over.
GENERATED_HEADER_RE = re.compile(r"^(?:com|java|javax|org|cn1)_[A-Za-z0-9_]*\.h$")
MISSING_HEADER_RE = re.compile(r"fatal error: '([^']+)' file not found")
MAX_STUB_ROUNDS = 40

# Proves the differ can see an SDK-caused change at all.
#
# Derived from the SDK versions rather than naming a real deprecation. It used to compile a
# call to [UIApplication canOpenURL:], deprecated in iOS 27, which only produces a delta for
# a pair that straddles 27: two pre-27 SDKs emit nothing and 27-vs-28 emits it twice, and in
# both cases the self-test fails and takes the whole gate with it. Since this check activates
# automatically whenever a runner has two Xcodes, a routine image bump would have broken it
# before it looked at the port. __IPHONE_OS_VERSION_MAX_ALLOWED is the SDK describing itself,
# so the same probe works for any pair, in either direction, forever.
PROBE_MARKER = "cn1-sdk-delta-probe"
PROBE_SOURCE = """#include <Availability.h>
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= %d
#warning cn1-sdk-delta-probe
#endif
"""


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, **kw)


def fail(msg):
    print("ERROR: %s" % msg, file=sys.stderr)
    sys.exit(2)


def discover_sdks(developer_dirs):
    """Return every iPhoneOS sdk found under the given developer dirs, keyed by version."""
    found = {}
    for dev in developer_dirs:
        sdk_root = os.path.join(dev, "Platforms", "iPhoneOS.platform", "Developer", "SDKs")
        if not os.path.isdir(sdk_root):
            continue
        for name in os.listdir(sdk_root):
            path = os.path.join(sdk_root, name)
            # Skip the unversioned symlink; the versioned name carries the answer.
            if os.path.islink(path) or not name.endswith(".sdk"):
                continue
            settings = os.path.join(path, "SDKSettings.plist")
            if not os.path.isfile(settings):
                continue
            res = run(["/usr/libexec/PlistBuddy", "-c", "Print :Version", settings])
            if res.returncode == 0:
                found[res.stdout.strip()] = path
    return found


def defines_for(gates, config_defines):
    """The exact -D set a sweep compiles with.

    Shared with the renderer self-check on purpose. When the check built its own list it
    reported the configurations as distinct while both sweeps were compiling the same
    Metal path, because CN1_USE_METAL had been harvested as a feature gate and applied to
    both. A self-check that does not mirror the real invocation can only confirm itself.
    """
    return gates + EXTRA_DEFINES + config_defines


def clang_for_sdk(sdk_path, dev_dirs):
    """The clang belonging to the Xcode that ships `sdk_path`.

    ONE compiler is used for both runs, on purpose: each Xcode's own clang would fold
    compiler changes into the result and the output would stop being about the SDK. But
    WHICH one matters, and the obvious choice is wrong. This used to take the first clang
    found while walking /Applications in name order, and "Xcode.app" sorts before
    "Xcode27.app", so the older compiler was handed the newer SDK -- the one direction that
    misfires, since new SDK headers can use attributes an old clang does not know. Those
    errors appear only under the new SDK and are indistinguishable from the regressions
    this script exists to report. A newer clang on older headers is the safe direction.

    Falls back to any clang under the known developer directories, so a machine with an
    unusual layout still runs rather than refusing.
    """
    resolved = os.path.realpath(sdk_path)
    owners = [d for d in dev_dirs if resolved.startswith(os.path.realpath(d) + os.sep)]
    # An SDK lives at <developer dir>/Platforms/X.platform/Developer/SDKs/Y.sdk, so the path
    # names its own Xcode. Needed because dev_dirs only knows about $DEVELOPER_DIR and
    # /Applications: an --old-sdk or --new-sdk pointing at a beta mounted elsewhere, or an
    # Xcode in /opt, left owners empty and fell through to whichever compiler /Applications
    # happened to offer -- reintroducing the compiler/SDK mismatch this function exists to
    # prevent, and silently.
    marker = os.sep + "Platforms" + os.sep
    if marker in resolved:
        owners.insert(0, resolved[:resolved.index(marker)])
    for dev in owners + list(dev_dirs):
        cand = os.path.join(dev, "Toolchains", "XcodeDefault.xctoolchain", "usr", "bin", "clang")
        if os.path.isfile(cand):
            return cand
    return None


def default_developer_dirs():
    dirs = []
    env = os.environ.get("DEVELOPER_DIR")
    if env:
        dirs.append(env)
    for entry in sorted(os.listdir("/Applications")):
        if entry.lower().startswith("xcode") and entry.endswith(".app"):
            dirs.append(os.path.join("/Applications", entry, "Contents", "Developer"))
    seen, out = set(), []
    for d in dirs:
        if d not in seen and os.path.isdir(d):
            seen.add(d)
            out.append(d)
    return out


def sdk_version(sdk_path):
    res = run(["/usr/libexec/PlistBuddy", "-c", "Print :Version",
               os.path.join(sdk_path, "SDKSettings.plist")])
    if res.returncode != 0:
        fail("cannot read the SDK version of %s" % sdk_path)
    return res.stdout.strip()


def version_key(v):
    return tuple(int(p) for p in re.findall(r"\d+", v))


def harvest_gates():
    """Collect the feature gates from the port sources -- the authority, not a copy of it."""
    gates = set()
    pattern = re.compile(r"^\s*//#define\s+(\S+)")
    for name in os.listdir(NATIVE_SOURCES):
        if not name.endswith((".h", ".m")):
            continue
        with open(os.path.join(NATIVE_SOURCES, name), "r", errors="replace") as fh:
            for line in fh:
                m = pattern.match(line)
                if m:
                    gates.add(m.group(1))
    unknown_denied = GATE_DENY - gates
    if unknown_denied:
        # A deny entry that matches nothing means a gate was renamed and the exclusion is
        # now silently covering a different name, or nothing at all.
        fail("GATE_DENY names no longer present in the port: %s" % ", ".join(sorted(unknown_denied)))
    return sorted(gates - GATE_DENY)


def discover_project_dir():
    """Find a generated ios-source tree.

    Nothing is compiled FROM here -- it supplies the ParparVM-generated headers
    (cn1_globals.h and friends) and the prefix header that the port's natives include.
    """
    candidates = []
    for sample in ("hellocodenameone", "fidelity-app", "cn1playground"):
        base = os.path.join(REPO_ROOT, "scripts", sample, "ios", "target")
        if not os.path.isdir(base):
            continue
        for entry in os.listdir(base):
            if not entry.endswith("-ios-source"):
                continue
            for sub in os.listdir(os.path.join(base, entry)):
                if sub.endswith("-src") and os.path.isdir(os.path.join(base, entry, sub)):
                    candidates.append(os.path.join(base, entry, sub))
    candidates.sort(key=lambda p: os.path.getmtime(p), reverse=True)
    return candidates[0] if candidates else None


# clang writes "file:line:col: <kind>: message"; note ": fatal error:" is its own kind.
DIAGNOSTIC_RE = re.compile(r":\d+:\d+: (fatal error|error|warning): ")

# The quoted symbol a deprecation is about, so N uses of one dead API group into one row.
SYMBOL_RE = re.compile(r"(?:warning|error): '([^']+)' is deprecated")


def normalize(line, project_dir):
    """Strip absolute paths but KEEP line:col -- both runs see byte-identical source, so a
    position is exact rather than noisy, and it distinguishes two hits in one file."""
    line = line.replace(project_dir.rstrip("/") + "/", "")
    line = line.replace(NATIVE_SOURCES.rstrip("/") + "/", "")
    return line.strip()


def parse_diagnostics(out, project_dir):
    """Clang output -> (normalized diagnostics, error count, fatal count).

    One parser for the real sweeps AND the self-test. The self-test used to substring-match
    clang's raw output, which proved only that CLANG behaves differently under the two SDKs.
    Everything that turns that into a finding -- this parser, normalize(), the set
    subtraction -- went untested, so a break in any of them would have left the probe happy
    and the run reporting OK.
    """
    diags, errors, fatal = [], 0, 0
    for line in out.splitlines():
        m = DIAGNOSTIC_RE.search(line)
        if not m:
            continue
        kind = m.group(1)
        # "fatal error" is the dangerous one: clang stops the translation unit there, so
        # every later diagnostic is missing rather than absent. Matching only ": error:"
        # misses it outright -- the text is ": fatal error:" -- and the file then counts
        # as clean while contributing nothing.
        if kind == "fatal error":
            fatal += 1
            errors += 1
        elif kind == "error":
            errors += 1
        diags.append(normalize(line, project_dir))
    return diags, errors, fatal


def compile_one(clang, sdk, project_dir, prefix_header, filename, defines, arc, target,
                stub_dir=None):
    src = os.path.join(NATIVE_SOURCES, filename)
    cache = os.path.join(tempfile.gettempdir(), "cn1-sdk-delta-modules")
    cmd = [
        clang, "-fsyntax-only", "-arch", "arm64", "-target", "arm64-apple-ios" + target,
        "-isysroot", sdk, "-fmodules", "-fmodules-cache-path=" + cache,
        "-fobjc-arc" if arc else "-fno-objc-arc",
        # The port's own headers first, then the generated tree for cn1_globals.h etc.
        "-I", NATIVE_SOURCES, "-I", project_dir,
        "-Wdeprecated-declarations",
        # A real build links these natives against a per-app ParparVM translation that
        # declares every com_codename1_* callback the app references. We compile without
        # one, so those calls are implicit and the values they return read as int. Both
        # runs see exactly the same set, so they cancel -- but left as errors they derail
        # the rest of the file and cost coverage, which is the opposite of the point.
        "-Wno-implicit-function-declaration", "-Wno-int-conversion",
    ]
    if prefix_header:
        cmd += ["-include", prefix_header]
    if stub_dir:
        cmd += ["-I", stub_dir]
    for d in defines:
        cmd += ["-D", d]
    cmd.append(src)
    res = run(cmd)
    out = res.stdout + res.stderr
    diags, errors, fatal = parse_diagnostics(out, project_dir)
    if res.returncode != 0 and errors == 0:
        # clang can exit nonzero with nothing this parser recognises: a frontend crash, a
        # signal, or a driver-level error such as an SDK it cannot open. Those carry no
        # "file:line:col: kind:" prefix, so both counters stayed at zero and the file was
        # recorded as clean -- which means every compile under the new SDK could fail this
        # way and the run would still satisfy the clean-file floor and print OK.
        errors += 1
        fatal += 1
        detail = out.strip().splitlines()
        diags.append("%s:0:0: fatal error: clang exited %d with no parseable diagnostic%s"
                     % (filename, res.returncode,
                        (" (" + detail[-1].strip() + ")") if detail else ""))
    return diags, errors, fatal


def configurations_differ(clang, sdk, project_dir, prefix_header, stub_dir, gates):
    """Confirm every non-baseline configuration really selects different code.

    Difference is decided on a DIGEST of the preprocessed output, not its line
    count. Equal counts do not mean equal content: a gated branch that swaps one
    implementation for another of the same length is genuinely different code,
    and judging it by length reported it as a duplicate configuration and aborted
    the run -- a green tree blocked by an edit that was fine.

    Returns (name, witness, baseline_lines, config_lines, identical) per
    configuration. The counts are carried for the human-readable report only;
    `identical` is the answer.
    """
    def preprocess(extra, witness):
        cmd = [clang, "-E", "-arch", "arm64", "-target", "arm64-apple-ios14.0",
               "-isysroot", sdk, "-fmodules",
               "-fmodules-cache-path=" + os.path.join(tempfile.gettempdir(),
                                                      "cn1-sdk-delta-modules"),
               "-fno-objc-arc", "-I", NATIVE_SOURCES, "-I", project_dir]
        if stub_dir:
            cmd += ["-I", stub_dir]
        if prefix_header:
            cmd += ["-include", prefix_header]
        for d in defines_for(gates, extra):
            cmd += ["-D", d]
        cmd.append(os.path.join(NATIVE_SOURCES, witness))
        out = run(cmd).stdout
        return (hashlib.sha256(out.encode("utf-8", "replace")).hexdigest(),
                len(out.splitlines()))

    baseline_defines = CONFIGURATIONS[0][1]
    checked = []
    for name, extra, witness in CONFIGURATIONS[1:]:
        base_digest, base_lines = preprocess(baseline_defines, witness)
        conf_digest, conf_lines = preprocess(extra, witness)
        checked.append((name, witness, base_lines, conf_lines,
                        base_digest == conf_digest))
    return checked

def synthesize_generated_stubs(clang, sdk, project_dir, prefix_header, files, all_defines,
                               arc_files, target, stub_dir, jobs):
    """Create empty headers for translated classes the sample app never caused to exist.

    Returns the sorted list of stubs created. Anything missing that does not look like a
    ParparVM-generated header is left alone so it still fails -- that is the case where a
    header really did disappear from the SDK, which is exactly what this tool is for.
    """
    created = set()
    for _ in range(MAX_STUB_ROUNDS):
        missing = set()
        with ThreadPoolExecutor(max_workers=jobs) as pool:
            futures = [
                pool.submit(compile_one, clang, sdk, project_dir, prefix_header, name,
                            all_defines, name in arc_files, target, stub_dir)
                for name in files
            ]
            for fut in futures:
                diags, _, fatal = fut.result()
                if not fatal:
                    continue
                for line in diags:
                    m = MISSING_HEADER_RE.search(line)
                    if m and GENERATED_HEADER_RE.match(os.path.basename(m.group(1))):
                        missing.add(m.group(1))
        missing -= created
        if not missing:
            break
        for header in sorted(missing):
            path = os.path.join(stub_dir, header)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w") as fh:
                fh.write("/* Synthesized by check-ios-sdk-deltas.py: ParparVM emits this\n"
                         "   header only for an app that references the class. Empty is\n"
                         "   enough -- the calls it would declare are already tolerated as\n"
                         "   implicit, and nothing here can mask an SDK change. */\n")
            created.add(header)
    return sorted(created)


def sdk_minimum_deployment_target(sdk_path):
    """The lowest deployment target this SDK accepts, or None.

    The same value AppleSdkFloor reads in the builder, and read the same way, so the sweep
    compiles at the target the build will actually use.
    """
    settings = os.path.join(sdk_path, "SDKSettings.plist")
    if not os.path.isfile(settings):
        return None
    res = run(["/usr/bin/plutil", "-extract",
               "SupportedTargets.iphoneos.MinimumDeploymentTarget", "raw", settings])
    value = res.stdout.strip()
    return value if res.returncode == 0 and value and value[0].isdigit() else None


def sdk_version_macro(version):
    """The SDK version as __IPHONE_OS_VERSION_MAX_ALLOWED spells it: 27.0 -> 270000."""
    parts = [int(x) for x in re.findall(r"\d+", version)[:3]]
    while len(parts) < 3:
        parts.append(0)
    return parts[0] * 10000 + parts[1] * 100 + parts[2]


def compile_probe(clang, sdk, target, threshold):
    tmp = tempfile.mkdtemp(prefix="cn1-sdk-probe-")
    try:
        src = os.path.join(tmp, "probe.m")
        with open(src, "w") as fh:
            fh.write(PROBE_SOURCE % threshold)
        res = run([clang, "-fsyntax-only", "-arch", "arm64",
                   "-target", "arm64-apple-ios" + target,
                   "-isysroot", sdk, "-fno-objc-arc", "-Wdeprecated-declarations", src])
        diags, _, _ = parse_diagnostics(res.stdout + res.stderr, tmp)
        return set(diags)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--old-sdk", help="Baseline iPhoneOS sdk path (default: second newest installed)")
    ap.add_argument("--new-sdk", help="iPhoneOS sdk path under test (default: newest installed)")
    ap.add_argument("--project-dir", help="A generated *-ios-source/*-src directory")
    ap.add_argument("--clang",
                    help="clang to use for BOTH runs. Defaults to the one belonging to "
                         "the Xcode that ships --new-sdk, which is the safe direction: a "
                         "newer compiler reads older headers, not the other way round.")
    ap.add_argument("--jobs", type=int, default=os.cpu_count() or 4)
    ap.add_argument("--skip-if-single-sdk", action="store_true",
                    help="Exit 0 with a note when only one iPhoneOS SDK is installed. For "
                         "CI, where this check is a no-op until a runner image carries the "
                         "next Xcode -- and must then start working without another edit.")
    ap.add_argument("--verbose", action="store_true")
    args = ap.parse_args()

    dev_dirs = default_developer_dirs()
    old_sdk, new_sdk = args.old_sdk, args.new_sdk
    if not (old_sdk and new_sdk):
        sdks = discover_sdks(dev_dirs)
        # An explicitly supplied SDK counts toward the pair. Requiring two DISCOVERED ones
        # refused the case this option exists for: --new-sdk on a beta mounted outside
        # /Applications, next to the single SDK the machine has installed.
        for explicit in (args.old_sdk, args.new_sdk):
            if explicit and os.path.isdir(explicit):
                sdks[sdk_version(explicit)] = explicit
        if len(sdks) < 2:
            if args.skip_if_single_sdk:
                print("[ios-sdk-deltas] only %d iPhoneOS SDK available (%s); nothing to "
                      "compare, skipping." % (len(sdks), ", ".join(sorted(sdks)) or "none"))
                return 0
            fail("need two iPhoneOS SDKs to compare; found %d (%s). Install a second Xcode "
                 "or pass --old-sdk/--new-sdk." % (len(sdks), ", ".join(sorted(sdks)) or "none"))
        ordered = sorted(sdks, key=version_key)
        if not new_sdk:
            new_sdk = sdks[ordered[-1]]
        if not old_sdk:
            # The newest SDK strictly BELOW the one under test, not simply the second
            # newest: with an explicit --new-sdk the closest baseline beneath it is the
            # one that isolates what that SDK changed.
            target = version_key(sdk_version(new_sdk))
            below = [v for v in ordered if version_key(v) < target]
            if not below:
                if args.skip_if_single_sdk:
                    print("[ios-sdk-deltas] no iPhoneOS SDK older than %s to compare "
                          "against; skipping." % sdk_version(new_sdk))
                    return 0
                fail("no iPhoneOS SDK older than %s to compare against; found %s."
                     % (sdk_version(new_sdk), ", ".join(sorted(sdks))))
            old_sdk = sdks[below[-1]]
    for label, path in (("--old-sdk", old_sdk), ("--new-sdk", new_sdk)):
        if not os.path.isdir(path):
            fail("%s is not a directory: %s" % (label, path))

    clang = args.clang or clang_for_sdk(new_sdk, dev_dirs)
    if not clang or not os.path.isfile(clang):
        fail("could not locate clang; pass --clang")

    project_dir = args.project_dir or discover_project_dir()
    if not project_dir or not os.path.isdir(project_dir):
        fail("no generated iOS source tree found. Run scripts/build-ios-app.sh first, or "
             "pass --project-dir <...-ios-source/...-src>.")

    gates = harvest_gates()
    clang_version = run([clang, "--version"]).stdout.splitlines()
    print("[ios-sdk-deltas] clang   : %s%s"
          % (clang, "  (%s)" % clang_version[0].strip() if clang_version else ""))
    print("[ios-sdk-deltas] old sdk : %s" % old_sdk)
    print("[ios-sdk-deltas] new sdk : %s" % new_sdk)
    print("[ios-sdk-deltas] project : %s" % project_dir)
    print("[ios-sdk-deltas] gates   : %d harvested from Ports/iOSPort/nativeSources" % len(gates))

    prefix_header = None
    for name in os.listdir(project_dir):
        if name.endswith("-Prefix.pch"):
            prefix_header = os.path.join(project_dir, name)
            break
    if not prefix_header:
        fail("no *-Prefix.pch in %s; without it the natives cannot see cn1_globals.h and "
             "most of each file would go unexamined." % project_dir)

    # Every native the port owns -- not a subset of one sample app's build, and not only
    # the Objective-C ones. ByteCodeTranslator puts .c into the generated project alongside
    # .m, and seven of them ship here (the Curve/Dasher/Renderer/Stroker geometry, Helpers.c,
    # cn1_debugger_objects.c). Selecting only .m meant an SDK regression in any of those
    # still produced an OK.
    present = sorted(n for n in os.listdir(NATIVE_SOURCES)
                     if n.endswith(".m") or n.endswith(".c"))
    if not present:
        fail("no .m files under %s" % NATIVE_SOURCES)
    print("[ios-sdk-deltas] prefix  : %s" % prefix_header)
    print("[ios-sdk-deltas] natives : %d port sources" % len(present))

    arc_files = set()
    for name in present:
        with open(os.path.join(NATIVE_SOURCES, name), "r", errors="replace") as fh:
            if "requires ARC" in fh.read():
                arc_files.add(name)
    print("[ios-sdk-deltas] arc     : %d file(s) built with -fobjc-arc (%s)"
          % (len(arc_files), ", ".join(sorted(arc_files)) or "none"))

    new_version = sdk_version(new_sdk)
    # The builder raises the app to the SDK's floor, so that -- not the bare CN1 default --
    # is what a default project ships against.
    sdk_floor = sdk_minimum_deployment_target(new_sdk)
    shipping_target = CN1_DEFAULT_DEPLOYMENT_TARGET
    if sdk_floor and sdk_version_macro(sdk_floor) > sdk_version_macro(shipping_target):
        shipping_target = sdk_floor
    sweeps = [
        # (label, deployment target). What blocks the run is the KIND of diagnostic, not
        # which sweep found it: an error is a broken build wherever it shows up, and a
        # deprecation is a warning wherever it shows up.
        ("shipping-floor", shipping_target),
        ("future-floor", new_version),
    ]

    # Self-test first: if the differ cannot see an SDK-caused difference, nothing it
    # reports afterwards means anything. The threshold is the newer SDK's own version, so
    # the probe warns under it and stays silent under the older one whatever the two are.
    old_version = sdk_version(old_sdk)
    if sdk_version_macro(new_version) <= sdk_version_macro(old_version):
        # Everything below reads "appears only under the newer SDK", so the arguments being
        # the wrong way round would report removals as additions. Caught here rather than
        # left to the probe, whose failure would describe a symptom and not this cause.
        fail("--new-sdk is %s and --old-sdk is %s: the new one must be the NEWER SDK."
             % (new_version, old_version))
    threshold = sdk_version_macro(new_version)
    probe_old = compile_probe(clang, old_sdk, new_version, threshold)
    probe_new = compile_probe(clang, new_sdk, new_version, threshold)
    # Subtracted exactly the way a sweep's findings are, so the parser, normalize() and the
    # set difference are all on the path being proved -- not just clang.
    probe_delta = [line for line in sorted(probe_new - probe_old) if PROBE_MARKER in line]
    if not probe_delta:
        fail("self-test did not fire: a probe keyed to __IPHONE_OS_VERSION_MAX_ALLOWED >= %d "
             "should survive subtraction as a finding present only under %s. It did not, so "
             "an empty result from the real sweeps would be a lie.\n"
             "  under %s: %s\n  under %s: %s"
             % (threshold, os.path.basename(new_sdk),
                os.path.basename(old_sdk), sorted(probe_old) or "<no diagnostics>",
                os.path.basename(new_sdk), sorted(probe_new) or "<no diagnostics>"))
    print("[ios-sdk-deltas] self-test: differ sees a delta only the newer SDK produces "
          "(__IPHONE_OS_VERSION_MAX_ALLOWED >= %d)" % threshold)

    # A gate that cannot fail is not a gate. Prove the reporting path still turns a
    # blocking finding into a non-zero exit, with output suppressed so the check is
    # invisible unless it breaks.
    import io
    import contextlib
    synthetic = {"probe.m:1:1: error: synthetic self-test finding": {"self-test"}}
    with contextlib.redirect_stdout(io.StringIO()):
        must_fail = report(synthetic, {}, new_sdk, new_version, shipping_target, False)
        must_pass = report({}, {}, new_sdk, new_version, shipping_target, False)
    if must_fail == 0 or must_pass != 0:
        fail("the reporting path no longer turns a blocking finding into a failure "
             "(blocking exited %s, clean exited %s). Every run would report success."
             % (must_fail, must_pass))
    print("[ios-sdk-deltas] self-test: a blocking finding exits non-zero")

    stub_dir = tempfile.mkdtemp(prefix="cn1-sdk-delta-stubs-")
    all_defines = defines_for(gates, [d for _, ds, _w in CONFIGURATIONS for d in ds])
    # Against both SDKs: a header missing only under the old one leaves that side truncated,
    # and a truncated BASELINE turns ordinary diagnostics into invented regressions.
    stubs = []
    for sdk in (new_sdk, old_sdk):
        stubs += synthesize_generated_stubs(clang, sdk, project_dir, prefix_header, present,
                                            all_defines, arc_files, shipping_target,
                                            stub_dir, args.jobs)
    stubs = sorted(set(stubs))
    print("[ios-sdk-deltas] stubs   : %d translated-class header(s) synthesized%s"
          % (len(stubs), (" (%s)" % ", ".join(stubs)) if args.verbose and stubs else ""))

    checked = configurations_differ(clang, new_sdk, project_dir, prefix_header, stub_dir, gates)
    for name, witness, base_lines, conf_lines, identical in checked:
        if identical or conf_lines == 0:
            reason = ("preprocesses to byte-identical output" if identical
                      else "preprocesses to nothing")
            fail("configuration '%s' no longer selects different code: %s %s under the "
                 "baseline and under it (%d vs %d lines). It is a duplicate of the "
                 "baseline, so the coverage it claims is not real."
                 % (name, witness, reason, base_lines, conf_lines))
    print("[ios-sdk-deltas] configs : %s"
          % ", ".join("%s (%s %d vs %d)" % (n, w, b, c)
                       for n, w, b, c, _ in checked))

    blocking, informational = {}, {}
    for sweep_name, target in sweeps:
        for config_name, config_defines, _witness in CONFIGURATIONS:
            defines = defines_for(gates, config_defines)
            results = {}
            for sdk_label, sdk in (("old", old_sdk), ("new", new_sdk)):
                diags, clean, truncated = [], 0, []
                with ThreadPoolExecutor(max_workers=args.jobs) as pool:
                    futures = {
                        name: pool.submit(compile_one, clang, sdk, project_dir,
                                          prefix_header, name, defines,
                                          name in arc_files, target, stub_dir)
                        for name in present
                    }
                    for name, fut in futures.items():
                        d, errs, fatal = fut.result()
                        diags.extend(d)
                        if fatal:
                            truncated.append(name)
                        if errs == 0:
                            clean += 1
                results[sdk_label] = (set(diags), clean, sorted(truncated))

            old_diags, _, old_truncated = results["old"]
            new_diags, new_clean, new_truncated = results["new"]
            # BOTH sides, not just the new one. A fatal error stops the translation unit, so
            # the rest of that file is not "clean" -- it is unexamined. On the new side that
            # hides regressions. On the OLD side it invents them: everything after the abort
            # is missing from the baseline, so the new SDK's perfectly ordinary diagnostics
            # for those lines subtract to nothing and get reported as additions it caused.
            # That asymmetry was reachable, because the generated stubs are synthesized
            # against the new SDK alone.
            truncated = sorted(set(old_truncated) | set(new_truncated))
            if truncated:
                sides = []
                if old_truncated:
                    sides.append("old")
                if new_truncated:
                    sides.append("new")
                fail("%d file(s) aborted on a fatal error under the %s SDK in %s/%s, so they "
                     "were only partially examined: %s\n"
                     "Supply the missing header, or add the gate that pulls it in to "
                     "GATE_DENY with the reason."
                     % (len(truncated), "/".join(sides), sweep_name, config_name,
                        ", ".join(truncated)))
            if new_clean < MIN_CLEAN_FILES:
                fail("only %d files compiled cleanly under the new SDK in %s/%s (floor is "
                     "%d). An empty delta here would mean nothing was compiled, not that "
                     "nothing broke." % (new_clean, sweep_name, config_name, MIN_CLEAN_FILES))

            added = sorted(new_diags - old_diags)
            removed = sorted(old_diags - new_diags)
            # Files that fail identically under BOTH SDKs are environmental, not SDK
            # regressions -- a stale generated header is the usual cause -- and they cancel
            # in the subtraction. They still cost coverage, so the count is printed rather
            # than left for someone to notice that "clean" was not all of them.
            environmental = len(present) - new_clean
            print("[ios-sdk-deltas] %-12s %-13s ios%-5s: %d/%d clean%s, %d old / %d new "
                  "diags, %d added"
                  % (sweep_name, config_name, target, new_clean, len(present),
                     "" if environmental == 0
                     else " (%d fail under BOTH sdks: environmental, cancels)" % environmental,
                     len(old_diags), len(new_diags), len(added)))
            if args.verbose and removed:
                for line in removed:
                    print("    gone: %s" % line)
            for line in added:
                kind_match = DIAGNOSTIC_RE.search(line)
                kind = kind_match.group(1) if kind_match else "warning"
                bucket = blocking if kind in ("error", "fatal error") else informational
                # The same finding surfaces under both renderer configurations and often
                # under both sweeps. Key on the diagnostic itself so it is reported once,
                # with the contexts it was seen in.
                bucket.setdefault(line, set()).add("%s/%s" % (sweep_name, config_name))

    return report(blocking, informational, new_sdk, new_version, shipping_target,
                  args.verbose)


def report(blocking, informational, new_sdk, new_version, shipping_target, verbose):
    """Print the findings and return the process exit code.

    Separated from main so the exit code is a unit that can be exercised without an Xcode.
    It was not, once: a refactor of the printing dropped the `if blocking` branch entirely
    and nothing read the map, so the gate returned 0 whatever it found. Every real run had
    zero blocking errors, which made the wrong answer look like the right one.
    """
    print()
    if informational:
        # One deprecated framework produces dozens of lines. Group by the message so the
        # report names root causes, and keep every location available under --verbose --
        # summarising is only honest if nothing is dropped.
        groups = {}
        for line in informational:
            m = SYMBOL_RE.search(line)
            key = m.group(1) if m else line
            groups.setdefault(key, []).append(line)
        print("%d deprecation(s) in %d group(s) inherited from %s."
              % (len(informational), len(groups), os.path.basename(new_sdk)))
        print("Nothing is broken at the shipping deployment floor of ios%s -- clang cannot"
              % shipping_target)
        print("even warn about an ios%s deprecation there -- but these are live the moment"
              % new_version)
        print("the floor rises, and they are invisible to every other check we have.")
        print()
        for key in sorted(groups, key=lambda k: (-len(groups[k]), k)):
            lines = sorted(groups[key])
            files = sorted({l.split(":")[0] for l in lines})
            print("  %s  (%d use%s in %s)"
                  % (key, len(lines), "" if len(lines) == 1 else "s", ", ".join(files)))
            shown = lines if verbose else lines[:1]
            for line in shown:
                print("      %s" % line)
            if not verbose and len(lines) > 1:
                print("      ... %d more; re-run with --verbose for every location"
                      % (len(lines) - 1))
        print()

    if blocking:
        print("FAIL: %d compile error(s) appear only under %s"
              % (len(blocking), os.path.basename(new_sdk)))
        for line in sorted(blocking):
            print("  %s" % line)
            print("      seen in: %s" % ", ".join(sorted(blocking[line])))
        print()
        print("Each is caused by the newer SDK. Fix the call, or -- if the behaviour is")
        print("deliberate -- record it in scripts/native-warnings/baseline-ios-sim-debug.txt")
        print("with the reason. Do not widen this script to hide it.")
        return 1

    print("OK: %s introduces no compile errors at the shipping deployment floor."
          % os.path.basename(new_sdk))
    return 0


if __name__ == "__main__":
    sys.exit(main())
