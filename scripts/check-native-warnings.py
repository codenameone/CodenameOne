#!/usr/bin/env python3
"""Classifies the compiler warnings in a native build log by who owns the code.

A ParparVM build compiles four completely different kinds of C into one binary:
the code the translator generated, the ParparVM runtime, the hand-written port
natives, and vendored third-party sources. Their warnings all land in one log,
undifferentiated, and the volume is such that a real defect -- an Apple API that
is deprecated today and deleted in two releases, a pointer/integer confusion in
generated code -- is invisible. Nothing in this tree has ever counted them.

This tool splits a build log by ownership so each group can be driven to zero and
held there. Ownership comes from the manifest the translator writes
(cn1-source-manifest.txt); it cannot be derived from the path, because every one
of those four kinds ends up in the same flat directory.

Held against a per-leg baseline, ratchet-style, in the manner of
scripts/check-cast-semantics.sh: the baseline records what was true the day the
gate went in, new entries are a failure, and an entry that stops reproducing must
be deleted rather than left to rot.

Exit codes:
  0  no findings outside the baseline
  1  new findings, or stale baseline entries, or a diagnostic that could not be
     attributed to any known origin
  2  the build this log came from did not compile everything the manifest lists,
     so the census would undercount and must not be trusted
"""
import argparse
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASELINE_DIR = os.path.join(ROOT, "scripts", "native-warnings")

# --- Diagnostic grammars ----------------------------------------------------
#
# Anchored at start of line so that source-snippet lines ("    1 | int x;") and
# caret lines ("      | ^") never match: both begin with whitespace, and neither
# carries a ": warning: " at column zero. The clang-cl grammar is separate only
# because MSVC puts the location in parentheses; it still carries [-Wflag],
# because clang-cl is clang.
GNU_RE = re.compile(
    r'^(?P<path>[^\s][^:]*(?::[^:\s][^:]*)*?):(?P<line>\d+):(?P<col>\d+):\s+'
    r'(?P<sev>warning|error|note):\s+(?P<msg>.*)$')
MSVC_RE = re.compile(
    r'^(?P<path>[A-Za-z]?[^\s(][^(]*)\((?P<line>\d+)(?:,(?P<col>\d+))?\):\s+'
    r'(?P<sev>warning|error|note):\s+(?P<msg>.*)$')
BARE_RE = re.compile(
    r'^(?:(?P<tool>ld|clang|clang\+\+|clang-cl|cc|gcc|ninja|xcodebuild|libtool)'
    r':\s+)?(?P<sev>warning|error):\s+(?P<msg>.*)$')

FLAG_RE = re.compile(r'\[(-W[^\]]+)\]\s*$')

# Xcode names the source it is about to compile; ninja and make announce the
# object. Either way this is how we learn what the build ACTUALLY compiled, as
# opposed to what it could have compiled.
# Xcode names the task, the output, then the source:
#   CompileC <obj> <src> normal arm64 objective-c com.apple.compilers... (in target ...)
# Verified against Xcode 26.3 output. CompileMetalFile and the assembler use the
# same shape, and a .metal that only CompileC were matched would look like a
# source the build skipped.
COMPILE_XCODE_RE = re.compile(
    r'^\s*(?:CompileC|CompileMetalFile|CompileAssembly)\s+(?:"[^"]*"|\S+)\s+'
    r'(?P<src>"[^"]+"|\S+)\s+normal\b')
# CMake's two generators announce the same thing with different progress
# prefixes -- ninja counts jobs ("[7/91]"), make counts percent ("[  3%]") -- and
# the prefix is absent entirely when progress reporting is off. One optional group
# covers all three; splitting them into separate patterns is how the percent form
# got missed the first time, and a missed compile line reads as "the build
# compiled nothing", which is a hard failure rather than a wrong number.
COMPILE_CMAKE_RE = re.compile(
    r'^\s*(?:\[\s*(?:\d+/\d+|\d+%)\s*\]\s*)?'
    r'(?:Building|Compiling)\s+\S+\s+object\s+(?P<obj>\S+)')

SOURCE_EXTS = (".m", ".mm", ".c", ".cc", ".cpp", ".cxx", ".metal", ".S", ".s")
HEADER_EXTS = (".h", ".hh", ".hpp")

# Provenance rules for files the manifest does not name. These are rules about
# WHERE a file lives, not a list of files: a new SDK header or a new pod is
# covered without anyone editing this.
SDK_MARKERS = (
    "/Applications/Xcode", "/Library/Developer/", "/usr/include/", "/usr/lib/clang/",
    ".sdk/", "/Toolchains/", "/usr/local/include/", "/MacOSX.platform/",
    "/iPhoneOS.platform/", "/iPhoneSimulator.platform/", "/AppleTVOS.platform/",
    "/WatchOS.platform/", "/lib/gcc/", "/mingw", "/msvc/", "/Windows Kits/",
)
VENDORED_MARKERS = (
    "/Pods/", "/SourcePackages/", "/Checkouts/", "/DerivedData/",
    "/node_modules/", "/.build/", "/third_party/", "/xwin/",
)

GATING_GROUPS = ("generated", "runtime", "port", "toolchain")
ALL_GROUPS = GATING_GROUPS + ("vendored", "sdk")

# Port source directories per leg, MOST SPECIFIC FIRST. Used to turn a bare file
# name from the manifest back into a repo-relative path, so the baseline names the
# file a human has to open.
#
# Order matters and is not a tiebreak of convenience: METALView.m exists in both
# Ports/MacPort and Ports/iOSPort/nativeSources, and for a macOS build it is the
# MacPort one that was compiled. The leg knows which port it built, so listing that
# port first states a fact rather than guessing. A name that appears twice inside a
# SINGLE port tree is still a hard error -- there the leg tells us nothing and
# picking one would blame a file nobody edited.
LEG_PORT_DIRS = {
    "ios-sim-debug": ["Ports/iOSPort/nativeSources"],
    "ios-device-release": ["Ports/iOSPort/nativeSources"],
    "macos": ["Ports/MacPort", "Ports/iOSPort/nativeSources"],
    "windows-clang-cl": ["Ports/WindowsPort"],
    "linux-cc": ["Ports/LinuxPort"],
    "clean-target": [],
}


def signature(msg):
    """Collapses a diagnostic message to its shape.

    'unused variable locals_3_' and 'unused variable locals_17_' are one defect in
    one emitter, not two findings, so the identifiers and numbers that differ
    between instances are replaced by '?'. Without this the baseline would churn on
    every commit that renumbered a local.
    """
    msg = FLAG_RE.sub("", msg).strip()
    msg = re.sub(r"'[^']*'", "?", msg)
    msg = re.sub(r'"[^"]*"', "?", msg)
    msg = re.sub(r'\b\d+(?:\.\d+)*\b', "?", msg)
    return re.sub(r"\s+", " ", msg).strip()


class Diagnostic(object):
    __slots__ = ("path", "line", "col", "flag", "msg", "group", "identity")

    def __init__(self, path, line, col, flag, msg):
        # Normalised to forward slashes at construction. The Windows leg emits
        # backslash paths and its log is read on whatever host runs the gate, so
        # os.path.basename on a POSIX box would otherwise hand back the entire
        # path and every Windows warning would be unattributable.
        self.path = path.replace("\\", "/")
        self.line = line
        self.col = col
        self.flag = flag
        self.msg = msg
        self.group = None
        self.identity = None

    @property
    def dedup_key(self):
        # The warning's OWN location, never the translation unit that provoked it.
        # A header warning is re-emitted once per including TU and once per
        # architecture; those are one defect, and counting them per TU would make
        # the number a function of how many files happen to include the header.
        return (self.path, self.line, self.col, self.flag, self.msg)

    @property
    def key(self):
        return "|".join((self.group, self.identity, self.flag, signature(self.msg)))


def parse_log(text):
    """Every warning in the log, deduplicated, plus the sources that were compiled."""
    seen = {}
    order = []
    compiled = set()
    for raw in text.splitlines():
        line = raw.rstrip("\r")

        m = COMPILE_XCODE_RE.match(line)
        if m:
            compiled.add(os.path.basename(m.group("src").strip('"')))
        else:
            m = COMPILE_CMAKE_RE.match(line)
            if m:
                obj = os.path.basename(m.group("obj"))
                # CMake names objects <source>.o / <source>.obj, so the source name
                # is recoverable; anything else is left alone and simply will not
                # match, which shows up as an incomplete build rather than silently
                # passing.
                for suffix in (".o", ".obj"):
                    if obj.endswith(suffix):
                        obj = obj[: -len(suffix)]
                        break
                compiled.add(obj)

        m = GNU_RE.match(line) or MSVC_RE.match(line)
        if m:
            if m.group("sev") != "warning":
                continue
            msg = m.group("msg")
            flag_m = FLAG_RE.search(msg)
            d = Diagnostic(m.group("path"), int(m.group("line")),
                           int(m.group("col") or 0),
                           flag_m.group(1) if flag_m else "<no-flag>", msg)
        else:
            m = BARE_RE.match(line)
            if not m or m.group("sev") != "warning":
                continue
            # No file: a linker or driver diagnostic. It still matters -- ThinLTO
            # puts real findings here -- but it belongs to no source.
            d = Diagnostic("<none>", 0, 0, "<no-flag>", m.group("msg"))

        if d.dedup_key not in seen:
            seen[d.dedup_key] = d
            order.append(d)
    return order, compiled


def read_manifest(path):
    """{file name: (origin, source)} from the translator's manifest."""
    entries = {}
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) != 3:
                raise SystemExit("malformed manifest line in %s: %s" % (path, line))
            entries[parts[0]] = (parts[1], parts[2])
    return entries


_PORT_INDEX = {}


def port_index(leg):
    """{file name: [repo-relative paths]} for the port trees this leg builds from.

    Built once per leg. The obvious spelling -- walk the tree looking for the name
    each time a diagnostic needs resolving -- is O(diagnostics x files), and a real
    census carries thousands of diagnostics.
    """
    if leg in _PORT_INDEX:
        return _PORT_INDEX[leg]
    index = {}
    for rel in LEG_PORT_DIRS.get(leg, []):
        base = os.path.join(ROOT, rel)
        if not os.path.isdir(base):
            continue
        here = {}
        for dirpath, _dirs, files in os.walk(base):
            for fn in files:
                here.setdefault(fn, []).append(
                    os.path.relpath(os.path.join(dirpath, fn), ROOT))
        # A more specific port tree already claimed this name; see LEG_PORT_DIRS.
        for fn, paths in here.items():
            index.setdefault(fn, paths)
    _PORT_INDEX[leg] = index
    return index


def resolve_port_path(name, leg):
    """Repo-relative path of a hand-written native, or None if it is not ours.

    Matched on file name because the origin recorded in the manifest is a staging
    path inside whatever built the app, not a path in this checkout. Two matches is
    a hard error rather than a guess: picking one would attribute a warning to a
    file nobody edited.
    """
    matches = port_index(leg).get(name, [])
    if len(matches) > 1:
        raise SystemExit(
            "ambiguous port file %r for leg %s: %s\nResolve by scoping LEG_PORT_DIRS; "
            "guessing would blame a file nobody edited." % (name, leg, ", ".join(sorted(matches))))
    return matches[0] if matches else None


def classify(diags, manifest, leg):
    """Assigns every diagnostic an owner. Returns the ones that have no owner."""
    unattributed = []
    for d in diags:
        if d.path == "<none>":
            d.group, d.identity = "toolchain", "<none>"
            continue
        name = os.path.basename(d.path)
        norm = d.path
        entry = manifest.get(name)
        if entry:
            origin, _source = entry
            if origin == "generated":
                # Keyed on the emitter, not the file: a generated file exists only
                # if its class survived elimination, and under concatenation there
                # are no per-class files at all.
                d.group, d.identity = "generated", "*"
            elif origin in ("runtime", "vendored"):
                d.group, d.identity = origin, name
            elif origin == "port":
                repo_path = resolve_port_path(name, leg)
                # A hand-written native we cannot find in this checkout came from a
                # cn1lib or the application, not from a port we maintain.
                d.group = "port" if repo_path else "vendored"
                d.identity = repo_path or name
            else:
                unattributed.append(d)
            continue
        # Vendored is tested first because its markers are the more specific ones.
        # "/Library/Developer/" matches a developer's own
        # ~/Library/Developer/Xcode/DerivedData, so checking SDK first labelled a
        # Swift package checkout as an Apple SDK header. Neither group gates, so
        # this only ever affected what the report claimed -- but a census nobody
        # believes is no better than no census.
        if any(marker in norm for marker in VENDORED_MARKERS):
            d.group, d.identity = "vendored", name
        elif any(marker in norm for marker in SDK_MARKERS):
            d.group, d.identity = "sdk", name
        else:
            unattributed.append(d)
    return unattributed


def coverage_path(leg):
    return os.path.join(BASELINE_DIR, "coverage-%s.txt" % leg)


def read_coverage(leg):
    """The sources the build that wrote this leg's baseline actually compiled."""
    path = coverage_path(leg)
    if not os.path.exists(path):
        return None
    names = set()
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line and not line.startswith("#"):
                names.add(line)
    return names


def write_coverage(leg, compiled):
    with open(coverage_path(leg), "w", encoding="utf-8") as fh:
        fh.write("# Sources compiled by the build that produced baseline-%s.txt.\n" % leg)
        fh.write("#\n")
        fh.write("# The gate fails when a later run compiles FEWER of these. An incremental\n")
        fh.write("# build recompiles nothing and reports no warnings, which reads exactly like\n")
        fh.write("# a clean codebase; comparing against what was covered once makes that\n")
        fh.write("# impossible to mistake for progress.\n")
        fh.write("#\n")
        fh.write("# Not the same as the manifest: the manifest lists every file in the\n")
        fh.write("# generated project, and a build legitimately compiles a subset of it.\n")
        fh.write("\n")
        for name in sorted(compiled):
            fh.write("%s\n" % name)


def check_completeness(manifest, compiled, leg):
    """Whether this build covered as much as the one the baseline came from.

    Two different questions live here, and conflating them is what made the first
    version of this unusable:

    - Did this build compile ANYTHING? An incremental build recompiles nothing and
      reports no warnings; so does the documented xcodebuild failure where a bad
      ARCHS override makes every target compile nothing while still copying
      resources. Both are indistinguishable from a clean codebase, and both are
      fatal to a census.
    - Did it compile everything the manifest lists? No, and it should not have to.
      The manifest names every file in the generated project, and a target
      legitimately builds a subset -- a .metal goes through a different task, a
      source can be excluded from the target. Failing on that would be demanding
      the wrong invariant.

    So the ratchet is on COVERAGE, measured against the build that wrote the
    baseline. It is exact, needs no threshold, and needs nobody to enumerate which
    files a target happens to include.
    """
    expected = {n for n in manifest if n.endswith(SOURCE_EXTS)}
    never_compiled = sorted(expected - set(compiled))
    previous = read_coverage(leg)
    regressed = sorted(previous - set(compiled)) if previous else []
    return never_compiled, sorted(expected), regressed


def baseline_path(leg):
    return os.path.join(BASELINE_DIR, "baseline-%s.txt" % leg)


def read_baseline(leg):
    path = baseline_path(leg)
    if not os.path.exists(path):
        return set()
    keys = set()
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            keys.add("|".join(line.split("|")[:4]))
    return keys


def write_baseline(leg, diags, config):
    path = baseline_path(leg)
    counts = {}
    for d in diags:
        if d.group in GATING_GROUPS:
            counts[d.key] = counts.get(d.key, 0) + 1
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("# Compiler warnings in the %s build, as of the day this gate was added.\n" % leg)
        fh.write("#\n")
        fh.write("# This is a ratchet, not an allow-list: new code must not add entries.\n")
        fh.write("# Delete an entry when the warning stops reproducing -- a stale entry is a\n")
        fh.write("# failure too, so the file cannot quietly describe a build nobody runs.\n")
        fh.write("#\n")
        fh.write("# A baseline is only comparable to a census taken from the SAME build. This\n")
        fh.write("# one came from:\n")
        for line in config.splitlines():
            fh.write("#   %s\n" % line)
        fh.write("#\n")
        fh.write("# Format: <group>|<identity>|<flag>|<message-signature>|<note>\n")
        fh.write("#\n")
        fh.write("# identity is a repo-relative path for port code, the file name for the\n")
        fh.write("# runtime, and '*' for generated code -- there the unit of authorship is the\n")
        fh.write("# emitter, not the file.\n")
        fh.write("#\n")
        fh.write("# A <no-flag> diagnostic cannot be -Wno-'d or -Werror='d on its own. The only\n")
        fh.write("# remedies for one are fixing it or a whole-file pragma.\n")
        fh.write("#\n")
        fh.write("# Regenerate with scripts/check-native-warnings.py --leg %s --write-baseline\n" % leg)
        fh.write("\n")
        for key in sorted(counts):
            fh.write("%s|%d instance(s) when the baseline was written; not yet triaged\n"
                     % (key, counts[key]))
    return len(counts)


def summarize(diags, out):
    by_group = {}
    for d in diags:
        by_group.setdefault(d.group, []).append(d)
    out.write("## Native warning census\n\n")
    out.write("| group | distinct | instances | gating |\n")
    out.write("|---|---|---|---|\n")
    for g in ALL_GROUPS:
        items = by_group.get(g, [])
        distinct = len({d.key for d in items})
        out.write("| %s | %d | %d | %s |\n"
                  % (g, distinct, len(items), "yes" if g in GATING_GROUPS else "no"))
    out.write("\n")
    for g in ALL_GROUPS:
        items = by_group.get(g, [])
        if not items:
            continue
        # Grouped by the BASELINE key, not by (flag, shape): for the runtime and the
        # ports the identity is the file, so the same flag in two files is two rows to
        # fix and two rows in the baseline. Collapsing them here would make this table
        # disagree with the "distinct" count above and understate the work.
        counts = {}
        files = {}
        for d in items:
            counts[d.key] = counts.get(d.key, 0) + 1
            files.setdefault(d.key, set()).add(os.path.basename(d.path))
        out.write("### %s\n\n" % g)
        out.write("Sorted by instance count: the top row is the single highest-leverage fix.\n\n")
        out.write("| instances | identity | flag | shape | seen in |\n")
        out.write("|---|---|---|---|---|\n")
        for key, n in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])):
            _group, identity, flag, sig = key.split("|", 3)
            names = sorted(files[key])
            shown = ", ".join(names[:3]) + (" (+%d more)" % (len(names) - 3) if len(names) > 3 else "")
            out.write("| %d | %s | %s | %s | %s |\n" % (n, identity, flag, sig[:80], shown))
        out.write("\n")


PROBE_FLAG = "-Wcn1-gate-probe"
PROBE_MESSAGE = "cn1 warning-gate probe"


def probe_target(manifest):
    """A runtime file this leg actually has, to hang the probe warning on.

    Not a hard-coded name: the clean target writes the runtime out as
    cn1_globals.c where the Apple targets keep cn1_globals.m, so any fixed
    spelling is unattributable on some leg -- and an unattributable probe fails
    for the wrong reason, which would look like the gate working when it is not.
    """
    for name in sorted(manifest):
        if manifest[name][0] == "runtime" and name.endswith(SOURCE_EXTS):
            return name
    raise SystemExit("--probe needs a runtime source in the manifest and found none")

FIXTURE = os.path.join(BASELINE_DIR, "parser-fixture.log")


def self_test():
    """Proves the parser before any baseline exists, and needs no compiler.

    Every branch that has a way to be wrong gets a line: a flagged and an unflagged
    diagnostic, the MSVC location format, the same header warning arriving from two
    translation units and from two architectures (which must collapse to one), a
    linker warning with no file, a note that must be dropped, and the snippet and
    caret lines that must not be mistaken for diagnostics.
    """
    with open(FIXTURE, encoding="utf-8") as fh:
        diags, compiled = parse_log(fh.read())
    got = {(os.path.basename(d.path), d.line, d.col, d.flag, signature(d.msg)) for d in diags}
    expected = {
        ("IOSNative.m", 4211, 9, "-Wdeprecated-declarations",
         "? is deprecated: first deprecated in iOS ?"),
        ("cn1_globals.m", 913, 12, "<no-flag>", "implicit declaration of function ?"),
        ("CN1Vpn.m", 77, 5, "-Wunused-variable", "unused variable ?"),
        ("cn1_globals.h", 2201, 30, "-Wsign-compare",
         "comparison of integer expressions of different signedness"),
        ("<none>", 0, 0, "<no-flag>",
         "object file was built for newer iOS version than being linked"),
        ("com_codename1_ui_Form.m", 1502, 17, "-Wunused-variable", "unused variable ?"),
    }
    problems = []
    for extra in sorted(got - expected):
        problems.append("parsed a line it should not have: %r" % (extra,))
    for missing in sorted(expected - got):
        problems.append("failed to parse: %r" % (missing,))
    # The header diagnostic appears three times in the fixture -- twice from
    # different TUs, once from a second architecture -- and must survive as one.
    header = [d for d in diags if os.path.basename(d.path) == "cn1_globals.h"]
    if len(header) != 1:
        problems.append("header warning deduped to %d entries, expected 1" % len(header))
    if not {"IOSNative.m", "cn1_globals.c", "cn1_virtual_thread.c",
            "CN1MetalShaders.metal"} <= compiled:
        problems.append("did not recognise the compile lines: %s" % sorted(compiled))
    if problems:
        for p in problems:
            print("self-test: %s" % p, file=sys.stderr)
        return 1
    print("self-test: %d diagnostics parsed, %d sources seen compiled -- OK"
          % (len(diags), len(compiled)))
    return 0


def check_baselines():
    """Structural check on every checked-in baseline, needing no compiler.

    A baseline is the only record of what was true when the gate went in, and it is
    edited by hand as warnings get fixed. These are the ways an edit goes wrong
    without anyone noticing: a dropped field, an empty note (an entry nobody
    justified), a group that never gates (so the row masks nothing and only
    misleads), or a typo in the group name that silently stops matching.
    """
    problems = []
    if not os.path.isdir(BASELINE_DIR):
        return 0
    for fn in sorted(os.listdir(BASELINE_DIR)):
        if not fn.startswith("baseline-") or not fn.endswith(".txt"):
            continue
        path = os.path.join(BASELINE_DIR, fn)
        with open(path, encoding="utf-8") as fh:
            for n, line in enumerate(fh, 1):
                line = line.rstrip("\n")
                if not line.strip() or line.startswith("#"):
                    continue
                parts = line.split("|")
                where = "%s:%d" % (fn, n)
                if len(parts) != 5:
                    problems.append("%s: expected 5 |-separated fields, got %d: %s"
                                    % (where, len(parts), line))
                    continue
                group, identity, flag, _sig, note = parts
                if group not in ALL_GROUPS:
                    problems.append("%s: unknown group %r (one of %s)"
                                    % (where, group, ", ".join(ALL_GROUPS)))
                if group not in GATING_GROUPS:
                    problems.append("%s: group %r never gates, so this entry masks nothing "
                                    "and only misleads -- delete it" % (where, group))
                if not identity.strip():
                    problems.append("%s: empty identity" % where)
                if not flag.startswith("-W") and flag != "<no-flag>":
                    problems.append("%s: %r is not a warning flag" % (where, flag))
                if not note.strip():
                    problems.append("%s: empty note -- every baselined warning needs a "
                                    "reason someone can read" % where)
    if problems:
        for p in problems:
            print("baseline-check: %s" % p, file=sys.stderr)
        return 1
    print("baseline-check: every checked-in baseline is well formed")
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--leg", help="which build this log came from, e.g. ios-sim-debug")
    ap.add_argument("--log", help="the build log to read")
    ap.add_argument("--manifest", help="cn1-source-manifest.txt from the same build")
    ap.add_argument("--write-baseline", action="store_true",
                    help="record the current findings as the new baseline")
    ap.add_argument("--report-only", action="store_true",
                    help="print the census and always exit 0")
    ap.add_argument("--allow-partial", action="store_true",
                    help="do not require the build to have compiled everything (never in CI)")
    ap.add_argument("--json", help="write every diagnostic to this file")
    ap.add_argument("--self-test", action="store_true", help="check the parser against its fixture")
    ap.add_argument("--check-baselines", action="store_true",
                    help="structural check on the checked-in baselines; needs no compiler")
    ap.add_argument("--probe", action="store_true",
                    help="inject one synthetic warning and assert the gate reacts to it")
    args = ap.parse_args()

    if args.self_test:
        return self_test() or check_baselines()
    if args.check_baselines:
        return check_baselines()

    for required in ("leg", "log", "manifest"):
        if not getattr(args, required):
            ap.error("--%s is required" % required)

    with open(args.log, encoding="utf-8", errors="replace") as fh:
        text = fh.read()
    manifest = read_manifest(args.manifest)
    probe_key = None
    if args.probe:
        target = probe_target(manifest)
        text += "\n%s:1:1: warning: %s [%s]\n" % (target, PROBE_MESSAGE, PROBE_FLAG)
        probe_key = "|".join(("runtime", target, PROBE_FLAG, PROBE_MESSAGE))

    diags, compiled = parse_log(text)

    never_compiled, expected, regressed = check_completeness(manifest, compiled, args.leg)
    if not args.allow_partial:
        if not compiled:
            print("FAIL: this log records no compilation at all, so an empty warning list "
                  "means nothing. A build that compiles nothing while still copying "
                  "resources looks exactly like this.", file=sys.stderr)
            return 2
        if regressed:
            print("FAIL: %d source(s) that the baselined build compiled were not compiled "
                  "by this one, so the census undercounts and a warning could disappear "
                  "without being fixed. Re-run against a cold build.\n  %s%s"
                  % (len(regressed), "\n  ".join(regressed[:40]),
                     "\n  ..." if len(regressed) > 40 else ""), file=sys.stderr)
            return 2
    print("coverage: %d source(s) compiled; %d of the %d in the manifest were not built by "
          "this target" % (len(compiled), len(never_compiled), len(expected)))
    if never_compiled:
        shown = ", ".join(never_compiled[:20])
        if len(never_compiled) > 20:
            shown += ", ... (%d more)" % (len(never_compiled) - 20)
        print("  not built by this target: %s" % shown)

    unattributed = classify(diags, manifest, args.leg)
    if unattributed:
        print("FAIL: %d diagnostics could not be attributed to any known origin. Every "
              "warning has an owner; add the provenance rule rather than dropping "
              "these.\n  %s" % (len(unattributed),
                                "\n  ".join(sorted({d.path for d in unattributed})[:20])),
              file=sys.stderr)
        return 1

    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump([{"path": d.path, "line": d.line, "col": d.col, "flag": d.flag,
                        "message": d.msg, "group": d.group, "identity": d.identity,
                        "key": d.key} for d in diags], fh, indent=1, sort_keys=True)

    summarize(diags, sys.stdout)
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            summarize(diags, fh)

    if args.write_baseline:
        n = write_baseline(args.leg, diags, "leg: %s\nlog: %s" % (args.leg, os.path.basename(args.log)))
        write_coverage(args.leg, compiled)
        print("wrote %d baseline entries to %s" % (n, baseline_path(args.leg)))
        print("wrote %d covered sources to %s" % (len(compiled), coverage_path(args.leg)))
        return 0

    gating = [d for d in diags if d.group in GATING_GROUPS]
    baseline = read_baseline(args.leg)
    seen_keys = {d.key for d in gating}
    new = sorted(seen_keys - baseline)
    stale = sorted(baseline - seen_keys)

    if args.probe:
        # The probe asserts the whole chain reacted -- parse, classify, key, diff --
        # on the real log with the real manifest. A gate that has gone blind (missing
        # manifest, wrong baseline path, everything silently bucketed as vendored)
        # fails here on the day it breaks rather than the day someone notices it
        # never fired.
        if probe_key not in seen_keys:
            print("FAIL: the injected probe warning did not come back as %r. The gate is "
                  "not classifying warnings correctly and is not gating." % probe_key,
                  file=sys.stderr)
            return 1
        if probe_key not in new:
            print("FAIL: the injected probe warning was not reported as new. The gate "
                  "would not fail on a newly introduced warning.", file=sys.stderr)
            return 1
        print("probe: the gate reacted to an injected warning as expected")
        return 0

    if args.report_only:
        print("\nreport-only: %d new, %d stale (not failing)" % (len(new), len(stale)))
        return 0

    status = 0
    if new:
        print("\nFAIL: %d warning kind(s) are not in the baseline for %s:" % (len(new), args.leg),
              file=sys.stderr)
        for k in new:
            print("  %s" % k, file=sys.stderr)
        status = 1
    if stale:
        print("\nFAIL: %d baseline entr(ies) for %s no longer reproduce. Delete them:"
              % (len(stale), args.leg), file=sys.stderr)
        for k in stale:
            print("  %s" % k, file=sys.stderr)
        status = 1
    if not status:
        print("\nOK: %d gating diagnostic(s), all baselined (%d entries)."
              % (len(gating), len(baseline)))
    return status


if __name__ == "__main__":
    sys.exit(main())
