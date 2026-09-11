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
# The column is OPTIONAL. "path:line: warning: ..." is a valid diagnostic that gcc and
# several tools emit, and requiring :line:col: dropped every one of them silently --
# the worst failure mode for a census, because the gate then reports clean.
GNU_RE = re.compile(
    r'^(?P<path>[^\s][^:]*(?::[^:\s][^:]*)*?):(?P<line>\d+):(?:(?P<col>\d+):)?\s+'
    r'(?P<sev>warning|error|note):\s+(?P<msg>.*)$')

# A diagnostic about a FILE with no line at all -- "/tmp/App.xcodeproj: warning: The iOS
# Simulator deployment target ...". Xcode emits these per project and they are as
# reproducible as any other; neither grammar above accepts them, and BARE_RE will not
# either, because the line starts with neither "warning:" nor a tool name.
PATH_ONLY_RE = re.compile(
    r'^(?P<path>/[^\s:][^:]*):\s+(?P<sev>warning|error|note):\s+(?P<msg>.*)$')
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

SOURCE_EXTS = (".m", ".mm", ".c", ".cc", ".cpp", ".cxx", ".metal", ".S", ".s", ".swift")

# Swift is announced differently from everything else: the architecture comes second,
# and the line then carries either a "Compiling\ A.swift,\ B.swift" summary followed by
# the paths, or the paths alone. So take every .swift path on the line rather than a
# fixed position. Without this, adding .swift to SOURCE_EXTS would make the completeness
# check demand a unit it could not see being compiled and fail every build -- a
# generated iOS project does put .swift files in Compile Sources, and the sample has one.
SWIFT_COMPILE_RE = re.compile(r'^\s*SwiftCompile\s')
SWIFT_PATH_RE = re.compile(r'(/[^\s\\]+\.swift)')
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
    # An .xcframework is unzipped into the build's own products directory before
    # its headers are compiled against, so those headers arrive under a path that
    # looks local and is not: TensorFlowLiteC's c_api.h reached the census this
    # way. Not ours, and not something an application author can fix.
    "/XCFrameworkIntermediates/", ".framework/Headers/",
)

# A companion watchOS or tvOS app embedded in an iOS project is translated by a
# SECOND, independent ParparVM run, which writes its output to a sibling -src
# directory and its own manifest that nothing stages. Its files are generated code
# by exactly the same argument as the main app's, and a warning in one is a defect
# in the same emitter, so it is attributed the same way rather than left homeless.
COMPANION_SRC_MARKERS = ("/watch-src/", "/tv-src/")

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
    # Quoted identifiers collapse whatever quotes the tool chose. gcc and Apple's
    # asset-catalog compiler use the Unicode pair -- "Accent color \u2018AccentColor\u2019 is
    # not present" -- and matching only the ASCII apostrophe left those identifiers in
    # the key, so every distinct name became its own baseline row and the file churned.
    msg = re.sub(r"[\u2018\u201c`]([^\u2019\u201d'`\"]*)[\u2019\u201d'`]", "?", msg)
    msg = re.sub(r"'[^']*'", "?", msg)
    msg = re.sub(r'"[^"]*"', "?", msg)
    msg = re.sub(r'\b\d+(?:\.\d+)*\b', "?", msg)
    msg = re.sub(r"\s+", " ", msg).strip()
    # "|" separates the baseline's fields, and a diagnostic can legitimately contain one
    # -- gcc's "suggest parentheses around arithmetic in operand of '|'" does. Left raw,
    # write_baseline() emits a six-field row, the reader's split truncates the key after
    # the fourth, and check_baselines() then rejects the file the tool itself wrote.
    return msg.replace("|", "\\u007c")


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


# A line that is nothing but a path fragment: no whitespace, contains a slash, and
# carries no diagnostic of its own. xcodebuild occasionally breaks a long
# diagnostic across two lines at an arbitrary column, leaving the path prefix on
# one line and the rest on the next -- measured at 8 occurrences in 92,129 warnings
# on the iOS leg. Rare, but it produced file names like "odename1_ui_Display.m"
# (com_c + odename1_ui_Display.m), which belong to no file that exists and so could
# not be attributed to anyone.
SPLIT_PREFIX_RE = re.compile(r'^\S*/\S*$')

# Lines that legitimately follow a diagnostic and must never be glued onto it:
# clang's source snippet and caret (both indented), the include-trace header, and
# a build task announcement.
# Lines that legitimately follow a diagnostic and must never be glued onto it:
# clang's source snippet ("  5646 |     code") and its caret ("       |    ^"),
# the include-trace header, and a build task announcement.
#
# Excluding ALL indented lines here was wrong, and cost a red build: a split can
# land immediately before a space, leaving the continuation as " [-Wunused-variable]",
# which is indented and is exactly the thing that needs joining. Only the shapes
# that are genuinely something else are excluded; the join is still only made when
# it completes a trailing flag, so nothing else can be glued on by accident.
CONTINUATION_EXCLUDE_RE = re.compile(
    r'^(?:\s*(?:\d+\s*)?\||In file included from\b|[A-Z][A-Za-z]+\s+/|\[\s*\d)')


def _parses(line):
    return bool(GNU_RE.match(line) or MSVC_RE.match(line))


def rejoin_split_lines(lines):
    """Puts diagnostics back together that the log transport broke in two.

    xcodebuild's output reaches the log through a pipe, and a long diagnostic
    occasionally arrives split at an arbitrary byte with the remainder on the next
    line -- measured at roughly 18 occurrences in 92,129 warnings on the iOS leg.
    It happens in two places, and both were found in real output rather than
    imagined:

    - in the PATH, leaving "com_c" on one line and
      "odename1_ui_Display.m:5646:5: warning: ..." on the next, which names a file
      that does not exist and so belongs to nobody;
    - in the MESSAGE, leaving "... warning: unuse" and then "d variable 'SP'
      [-Wunused-variable]", which parses fine and produces a truncated message
      shape -- a bogus baseline row that would never match again.

    Both joins are self-validating rather than guessed. A path join is only made
    when the result parses as a diagnostic at all; a message join only when it
    completes a trailing [-Wflag] that was absent before. Anything that does not
    satisfy that is left exactly as it came.
    """
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        nxt = lines[i + 1] if i + 1 < n else None

        if nxt is not None and not _parses(line) and SPLIT_PREFIX_RE.match(line) \
                and ": warning:" not in line and ": error:" not in line \
                and ": note:" not in line and _parses(line + nxt):
            out.append(line + nxt)
            i += 2
            continue

        if nxt is not None and _parses(line) and not FLAG_RE.search(line) \
                and not _parses(nxt) and not CONTINUATION_EXCLUDE_RE.match(nxt) \
                and FLAG_RE.search(line + nxt):
            out.append(line + nxt)
            i += 2
            continue

        out.append(line)
        i += 1
    return out


def parse_log(text):
    """Every warning in the log, deduplicated, plus the sources that were compiled."""
    seen = {}
    order = []
    compiled = set()
    lost = 0
    for raw in rejoin_split_lines([l.rstrip("\r") for l in text.splitlines()]):
        line = raw

        m = COMPILE_XCODE_RE.match(line)
        if m:
            compiled.add(os.path.basename(m.group("src").strip('"')))
        elif SWIFT_COMPILE_RE.match(line):
            for sp in SWIFT_PATH_RE.findall(line):
                compiled.add(os.path.basename(sp))
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

        m = GNU_RE.match(line) or MSVC_RE.match(line) or PATH_ONLY_RE.match(line)
        if m:
            if m.group("sev") != "warning":
                continue
            msg = m.group("msg")
            flag_m = FLAG_RE.search(msg)
            gd = m.groupdict()
            d = Diagnostic(gd["path"], int(gd.get("line") or 0),
                           int(gd.get("col") or 0),
                           flag_m.group(1) if flag_m else "<no-flag>", msg)
        else:
            m = BARE_RE.match(line)
            if not m or m.group("sev") != "warning":
                continue
            # A fileless diagnostic carrying a [-Wflag] is one of two things, and the
            # TOOL PREFIX separates them. With a prefix -- "clang: warning: argument
            # unused during compilation: ... [-Wunused-command-line-argument]" -- it is
            # a real driver diagnostic that clang emits exactly this way, reproducible
            # and worth gating on. Without one it is a file-scoped diagnostic whose path
            # the log transport dropped outright (bytes lost, not merely split, so
            # nothing can put it back); attributing that to the toolchain would be a lie
            # and baselining it would freeze a row that can never recur.
            if FLAG_RE.search(m.group("msg")) and not m.group("tool"):
                lost += 1
                continue
            # A linker or driver diagnostic. It still matters -- ThinLTO puts real
            # findings here -- but it belongs to no source. Keep its flag when it has
            # one: a tool-prefixed diagnostic like clang's
            # -Wunused-command-line-argument is gated on like any other, and reporting
            # it as <no-flag> would merge every driver warning into one baseline row.
            bare_msg = m.group("msg")
            bare_flag = FLAG_RE.search(bare_msg)
            d = Diagnostic("<none>", 0, 0,
                           bare_flag.group(1) if bare_flag else "<no-flag>", bare_msg)

        if d.dedup_key not in seen:
            seen[d.dedup_key] = d
            order.append(d)
    return order, compiled, lost


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
    if leg not in LEG_PORT_DIRS:
        # Silently treating an unknown leg as "no port trees" would resolve every
        # hand-written native to nothing, reclassify all of it as vendored, and drop
        # it out of the gating set -- the census would still print and still pass,
        # having quietly stopped checking the code most worth checking. A leg with
        # genuinely no port sources says so with an empty list, as clean-target does.
        raise SystemExit(
            "unknown leg %r: add it to LEG_PORT_DIRS naming the port trees it builds "
            "from (an empty list if it has none). Known legs: %s"
            % (leg, ", ".join(sorted(LEG_PORT_DIRS))))
    index = {}
    for rel in LEG_PORT_DIRS[leg]:
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
        if d.line == 0:
            # A path with no line is not a diagnostic about source: it is the build
            # system talking about a file or a target ("WatchImages.xcassets: warning:
            # Accent color 'AccentColor' is not present in any asset catalogs"). It has
            # no owner among the compiled sources, and it belongs with the other
            # build-system diagnostics rather than being blamed on whoever happens to
            # share its basename. It still gates -- it is a real, reproducible warning
            # that was simply invisible until the grammar accepted it.
            d.group, d.identity = "toolchain", name
            continue
        # Order matters three ways, and getting it wrong misattributes silently.
        #
        # VENDORED and SDK go FIRST, ahead of the manifest: the manifest is keyed on a
        # bare file name and names collide across trees, so a pod shipping Renderer.h
        # would otherwise match the iOS port's Renderer.h and be reported as our code.
        # A path under an SDK or a dependency checkout is definitive and needs no lookup.
        if any(marker in norm for marker in VENDORED_MARKERS):
            d.group, d.identity = "vendored", name
            continue
        if any(marker in norm for marker in SDK_MARKERS):
            d.group, d.identity = "sdk", name
            continue
        # The companion marker goes LAST, after the manifest. A watch or tv target is a
        # second translation, and the port natives, the ParparVM runtime and the bundled
        # SQLite are all copied into it as well -- watch-src/IOSNative.m is still the
        # port and watch-src/cn1_sqlite3_amalgamation.h is still vendored. Testing the
        # directory first reclassified 435 diagnostics in one real build as generated
        # code, which would have hidden port warnings behind an emitter key. Only a file
        # the phone manifest does not name is the companion's own translated output.
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
        if any(marker in norm for marker in COMPANION_SRC_MARKERS):
            d.group, d.identity = "generated", "*"
            continue
        # Everything else ran above, so this is a file the manifest does not name and no
        # provenance rule recognises.
        unattributed.append(d)
    return unattributed


def check_completeness(manifest, compiled):
    """Sources the manifest lists that this build did not compile.

    An incremental build recompiles nothing and reports no warnings, which reads
    exactly like a clean codebase; so does the documented xcodebuild failure where
    a bad ARCHS override makes every target compile nothing while still copying
    resources. Both are fatal to a census and both must be impossible to mistake
    for progress.

    Measured against the manifest from the SAME build, deliberately. An earlier
    version compared against the set of sources the baselined build compiled, and
    two real runs of the same leg compiled 3147 and 3156 sources -- the app's
    translated surface moves a little between runs, so a cross-run comparison
    couples the gate to something that legitimately changes. Each of those runs
    compiled 100% of its own manifest, which is the invariant that actually holds
    and the one worth enforcing.
    """
    expected = {n for n in manifest if n.endswith(SOURCE_EXTS)}
    return sorted(expected - set(compiled)), sorted(expected)


# KNOWN LIMITATION, stated rather than left to be discovered: completeness is measured
# against the PHONE manifest only. An embedded watch or tv app is a second, independent
# translation with a manifest of its own that nothing stages, so a build that compiled
# the phone target fully and the companion not at all passes this check.
#
# Diagnostics from the companion are still attributed -- watch-src/tv-src files resolve
# through the manifest by name, and the companion's own translated classes fall to the
# COMPANION_SRC_MARKERS rule -- so nothing is misreported. What is not verified is that
# the companion compiled everything it has.
#
# Closing it means staging the companion's cn1-source-manifest.txt alongside the phone's
# and checking both; that is a change to build-ios-app.sh and the builders, not to this
# tool, which is why it is recorded here instead of being half-done.


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


# Rows per group in the rendered table. The JSON dump carries everything; this
# only bounds what a human is asked to scroll, and keeps the GitHub step summary
# under its 1MB ceiling -- past which GitHub drops the whole summary, so an
# unbounded table would cost the entire report rather than its tail.
MAX_ROWS_PER_GROUP = 60


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
        ranked = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))
        for key, n in ranked[:MAX_ROWS_PER_GROUP]:
            _group, identity, flag, sig = key.split("|", 3)
            names = sorted(files[key])
            shown = ", ".join(names[:3]) + (" (+%d more)" % (len(names) - 3) if len(names) > 3 else "")
            out.write("| %d | %s | %s | %s | %s |\n" % (n, identity, flag, sig[:80], shown))
        if len(ranked) > MAX_ROWS_PER_GROUP:
            hidden = ranked[MAX_ROWS_PER_GROUP:]
            out.write("\n%d further row(s) not shown, %d instance(s) between them. "
                      "They are in the --json dump; this table is truncated for length, "
                      "not because the tail was dropped from the census.\n"
                      % (len(hidden), sum(n for _k, n in hidden)))
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
        diags, compiled, lost = parse_log(fh.read())
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
        # Rejoined from a path split; the fragment alone named no real file.
        ("com_codename1_ui_Display.m", 5646, 5, "-Wunused-variable", "unused variable ?"),
        # Rejoined from a message split; unjoined this reads "unuse".
        ("com_codename1_ui_Form.m", 912, 9, "-Wunused-variable", "unused variable ?"),
        # A real fileless build-system warning, kept.
        ("<none>", 0, 0, "<no-flag>",
         "Skipping duplicate build file in Compile Sources build phase"),
        # Rejoined across an INDENTED continuation. Unjoined this keeps a truncated
        # message and loses its flag, which is what reddened CI once.
        ("com_codename1_ui_Button.m", 77, 9, "-Wunused-variable", "unused variable ?"),
        # Genuinely unflagged, and its snippet/caret must NOT have been glued on.
        ("cn1_globals.m", 42, 3, "<no-flag>", "implicit declaration of function ?"),
        # No column: "path:line: warning:". Dropped entirely before.
        ("cn1_globals.c", 77, 0, "-Wimplicit-function-declaration",
         "implicit declaration of function ?"),
        # A file with no line at all, as Xcode emits per project.
        ("HelloApp.xcodeproj", 0, 0, "<no-flag>",
         "The iOS Simulator deployment target is set to ?"),
        # A real driver warning: flagged AND tool-prefixed, so it is kept rather than
        # counted as a path the transport lost.
        ("<none>", 0, 0, "-Wunused-command-line-argument",
         "argument unused during compilation: ?"),
        # The delimiter in a message is escaped so the baseline row stays five fields.
        ("cn1_globals.c", 88, 5, "-Wparentheses",
         "suggest parentheses around arithmetic in operand of ?"),
    }
    problems = []
    for extra in sorted(got - expected):
        problems.append("parsed a line it should not have: %r" % (extra,))
    for missing in sorted(expected - got):
        problems.append("failed to parse: %r" % (missing,))
    # The header diagnostic appears three times in the fixture -- twice from
    # different TUs, once from a second architecture -- and must survive as one.
    if lost != 1:
        problems.append("expected exactly 1 truncation-lost diagnostic, got %d" % lost)
    header = [d for d in diags if os.path.basename(d.path) == "cn1_globals.h"]
    if len(header) != 1:
        problems.append("header warning deduped to %d entries, expected 1" % len(header))
    if not {"IOSNative.m", "cn1_globals.c", "cn1_virtual_thread.c",
            "CN1MetalShaders.metal",
            # Both paths off one SwiftCompile line, not just the first.
            "SwiftKotlinNativeImpl.swift", "CN1WatchApp.swift"} <= compiled:
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

    diags, compiled, lost = parse_log(text)

    never_compiled, expected = check_completeness(manifest, compiled)
    if not args.allow_partial:
        if not compiled:
            print("FAIL: this log records no compilation at all, so an empty warning list "
                  "means nothing. A build that compiles nothing while still copying "
                  "resources looks exactly like this.", file=sys.stderr)
            return 2
        if never_compiled:
            print("FAIL: %d of the %d sources in this build's own manifest were never "
                  "compiled, so the census undercounts. Either the build was incremental "
                  "-- re-run it cold -- or a source has been excluded from the target, "
                  "in which case say so here rather than letting the count drift.\n  %s%s"
                  % (len(never_compiled), len(expected), "\n  ".join(never_compiled[:40]),
                     "\n  ..." if len(never_compiled) > 40 else ""), file=sys.stderr)
            return 2
    if lost:
        print("note: %d diagnostic(s) lost their file to log truncation and are not "
              "attributed to anyone; they are reported here and never baselined." % lost)
    print("coverage: %d source(s) compiled, covering all %d in this build's manifest"
          % (len(compiled), len(expected)))

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
        print("wrote %d baseline entries to %s" % (n, baseline_path(args.leg)))
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
